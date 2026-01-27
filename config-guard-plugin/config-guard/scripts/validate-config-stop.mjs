import fs from 'node:fs/promises';
import path from 'node:path';
import { getRepoRoot, getChangedFiles } from './git-status.mjs';
import { detectConfigFiles } from './detect-config-files.mjs';
import { parseProperties } from './validators/properties-parse.mjs';
import { parseYaml } from './validators/yaml-parse.mjs';
import { parseJson } from './validators/json-parse.mjs';
import { collectMissingRequiredKeys, flattenObject, runPolicyChecks } from './validators/policy-checks.mjs';
import { inferEnv } from './env-infer.mjs';
import { reviewFileWithClaude } from './claude-review.mjs';
import { formatBlockerReport } from './report.mjs';

const DEFAULT_CONFIG = {
  version: 1,
  includePaths: ['.'],
  fileGlobs: [],
  prodFilePatterns: ['*-prod.*', 'prod.*'],
  envLabels: ['dev', 'uat', 'sit', 'staging', 'prod'],
  profileKeys: ['spring.profiles.active', 'spring.config.activate.on-profile', 'app.env', 'environment'],
  requiredKeys: {
    dev: [],
    uat: [],
    sit: [],
    staging: [],
    prod: []
  },
  uatImpliesProdKeys: true,
  uatOnlyKeys: [],
  urlRules: {
    keywordHeuristics: true,
    keywordsByEnv: {
      dev: ['dev'],
      uat: ['uat'],
      sit: ['sit'],
      staging: ['staging', 'stage'],
      prod: ['prod', 'production']
    },
    allowHostsByEnv: {},
    denyHostsByEnv: {}
  },
  checks: {
    debugInProd: 'warn',
    hardcodedSecrets: 'warn',
    unknownKeys: 'off',
    deprecatedKeys: 'off'
  },
  knownKeys: [],
  deprecatedKeys: []
};

const MAX_CLAUDE_REVIEW_BYTES = 200_000;

function safeJsonParse(input) {
  try {
    return JSON.parse(input);
  } catch {
    return null;
  }
}

async function loadConfig(repoRoot) {
  const configPath = path.join(repoRoot, '.config-guard.json');
  try {
    const content = await fs.readFile(configPath, 'utf8');
    return { ...DEFAULT_CONFIG, ...JSON.parse(content) };
  } catch {
    return DEFAULT_CONFIG;
  }
}

function getParserForFile(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  if (ext === '.properties') return 'properties';
  if (ext === '.yml' || ext === '.yaml') return 'yaml';
  if (ext === '.json') return 'json';
  return null;
}

function normalizeData(parserType, result) {
  if (!result || !result.data) return {};
  if (parserType === 'properties') return result.data;
  return flattenObject(result.data);
}

async function main() {
  const input = await new Promise((resolve) => {
    let data = '';
    process.stdin.on('data', (chunk) => {
      data += chunk.toString();
    });
    process.stdin.on('end', () => resolve(data));
  });

  const parsedInput = safeJsonParse(input) || {};
  const cwd = parsedInput.cwd || process.cwd();
  const repoRoot = await getRepoRoot(cwd);
  if (!repoRoot) return;

  const config = await loadConfig(repoRoot);
  const changedFiles = await getChangedFiles(repoRoot);
  if (changedFiles.length === 0) return;

  const configFiles = detectConfigFiles({ files: changedFiles, config, repoRoot });
  if (configFiles.length === 0) return;

  const fileSummaries = [];
  const generalWarnings = [];
  const generalBlockers = [];
  const uatKeys = new Set();
  const prodKeys = new Set();
  let hasProdFile = false;

  for (const filePath of configFiles) {
    const fileBlockers = [];
    const fileWarnings = [];
    let content = '';
    try {
      content = await fs.readFile(filePath, 'utf8');
    } catch {
      fileWarnings.push('Failed to read file content.');
      continue;
    }

    const parserType = getParserForFile(filePath);
    let parseResult = null;
    if (parserType === 'properties') parseResult = parseProperties(content);
    if (parserType === 'yaml') parseResult = parseYaml(content);
    if (parserType === 'json') parseResult = parseJson(content);

    if (parseResult?.errors?.length) {
      fileBlockers.push(...parseResult.errors.map((err) => `Parse error: ${err}`));
    }
    if (parseResult?.warnings?.length) {
      fileWarnings.push(...parseResult.warnings);
    }

    const flatData = normalizeData(parserType, parseResult);
    const envResult = inferEnv({ filePath, flatData, config });
    if (envResult.warnings.length > 0) {
      fileWarnings.push(...envResult.warnings);
    }
    const env = envResult.env;

    if (env) {
      const requiredBlockers = collectMissingRequiredKeys(env, flatData, config);
      fileBlockers.push(...requiredBlockers);
    }

    if (env === 'uat') {
      Object.keys(flatData).forEach((key) => uatKeys.add(key));
    }
    if (env === 'prod') {
      hasProdFile = true;
      Object.keys(flatData).forEach((key) => prodKeys.add(key));
    }

    const policyResult = runPolicyChecks({ filePath, env, flatData, content, config });
    fileBlockers.push(...policyResult.blockers);
    fileWarnings.push(...policyResult.warnings);

    if (content.length <= MAX_CLAUDE_REVIEW_BYTES) {
      const claudeResult = await reviewFileWithClaude({ filePath, env, content });
      if (claudeResult.warning) {
        fileWarnings.push(claudeResult.warning);
      }
      for (const issue of claudeResult.issues || []) {
        if (issue.severity === 'blocker') {
          fileBlockers.push(`Claude: ${issue.message} (${issue.suggested_fix || 'no fix provided'})`);
        } else if (issue.severity === 'warn') {
          fileWarnings.push(`Claude: ${issue.message}`);
        }
      }
    } else {
      fileWarnings.push('Skipped Claude review due to file size.');
    }

    if (fileWarnings.length > 0) {
      generalWarnings.push(`${path.basename(filePath)}: ${fileWarnings.join(' | ')}`);
    }

    if (fileBlockers.length > 0) {
      fileSummaries.push({ file: path.basename(filePath), blockers: fileBlockers });
    }
  }

  if (config.uatImpliesProdKeys && uatKeys.size > 0) {
    const uatOnly = new Set(config.uatOnlyKeys || []);
    if (!hasProdFile) {
      generalWarnings.push('UAT keys present but no PROD config changed; cannot verify PROD parity.');
    } else {
      const missing = Array.from(uatKeys).filter((key) => !uatOnly.has(key) && !prodKeys.has(key));
      if (missing.length > 0) {
        generalBlockers.push(`Prod config missing keys present in UAT: ${missing.slice(0, 10).join(', ')}`);
      }
    }
  }

  if (generalBlockers.length > 0) {
    fileSummaries.push({ file: 'UAT/PROD parity', blockers: generalBlockers });
  }

  if (fileSummaries.length > 0) {
    const reason = formatBlockerReport({ fileSummaries, generalWarnings });
    process.stdout.write(JSON.stringify({ decision: 'block', reason }));
  }
}

main().catch(() => {
  process.exit(0);
});
