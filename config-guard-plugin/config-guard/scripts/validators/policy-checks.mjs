import path from 'node:path';

export function flattenObject(input, prefix = '', output = {}) {
  if (input && typeof input === 'object' && !Array.isArray(input)) {
    for (const [key, value] of Object.entries(input)) {
      const nextPrefix = prefix ? `${prefix}.${key}` : key;
      flattenObject(value, nextPrefix, output);
    }
  } else {
    output[prefix] = input;
  }
  return output;
}

export function collectMissingRequiredKeys(env, flatData, config) {
  const required = config.requiredKeys?.[env] || [];
  const missing = required.filter((key) => !(key in flatData));
  return missing.map((key) => `Missing required key: ${key}`);
}

function extractUrls(content) {
  const regex = /https?:\/\/[^\s"']+/gi;
  return content.match(regex) || [];
}

function getHostname(urlString) {
  try {
    return new URL(urlString).hostname.toLowerCase();
  } catch {
    return null;
  }
}

function isDebugEnabled(key, value) {
  if (typeof value === 'string') {
    if (value.toLowerCase() === 'debug') return true;
    if (value.toLowerCase() === 'true' && key.toLowerCase().includes('debug')) return true;
  }
  if (typeof value === 'boolean' && value === true && key.toLowerCase().includes('debug')) return true;
  if (key.toLowerCase().startsWith('logging.level') && String(value).toLowerCase() === 'debug') return true;
  return false;
}

function looksLikeSecretKey(key) {
  return /(password|secret|token|apikey|api[_-]?key|credential|(^|[._-])key($|[._-]))/i.test(key);
}

function isPlaceholder(value) {
  return /^\s*(\$\{[^}]+\}|\{\{[^}]+\}\}|vault:)/i.test(String(value));
}

function maskValue(value) {
  if (value === null || value === undefined) return value;
  if (typeof value !== 'string') return value;
  if (value.length <= 4) return '***';
  return `${value.slice(0, 2)}***${value.slice(-2)}`;
}

export function runPolicyChecks({ filePath, env, flatData, content, config }) {
  const blockers = [];
  const warnings = [];
  const urlRules = config.urlRules ?? {};
  const keywordHeuristics = urlRules.keywordHeuristics !== false;
  const keywordsByEnv = urlRules.keywordsByEnv ?? {
    dev: ['dev'],
    uat: ['uat'],
    sit: ['sit'],
    staging: ['staging', 'stage'],
    prod: ['prod', 'production']
  };

  if (env === 'prod') {
    const urls = extractUrls(content);
    const denyHosts = (urlRules.denyHostsByEnv?.prod || []).map((host) => host.toLowerCase());
    const allowHosts = (urlRules.allowHostsByEnv?.prod || []).map((host) => host.toLowerCase());

    for (const urlString of urls) {
      const hostname = getHostname(urlString) || '';
      const isDenied = hostname && denyHosts.includes(hostname);
      if (isDenied) {
        blockers.push(`Prod config references denied host: ${hostname}`);
        continue;
      }

      const keywordMatch = keywordHeuristics
        ? ['dev', 'uat', 'sit', 'staging']
            .flatMap((label) => keywordsByEnv[label] || [label])
            .some((keyword) => urlString.toLowerCase().includes(keyword))
        : false;

      if (keywordMatch) {
        blockers.push(`Prod config URL appears non-prod: ${urlString}`);
      } else if (allowHosts.length > 0 && hostname && !allowHosts.includes(hostname)) {
        warnings.push(`Prod URL host not in allow-list: ${hostname}`);
      }
    }

    if ((config.checks?.debugInProd ?? 'warn') === 'warn') {
      for (const [key, value] of Object.entries(flatData)) {
        if (isDebugEnabled(key, value)) {
          warnings.push(`Debug flag enabled for ${key} in prod.`);
          break;
        }
      }
    }
  }

  if ((config.checks?.hardcodedSecrets ?? 'warn') === 'warn') {
    for (const [key, value] of Object.entries(flatData)) {
      if (!looksLikeSecretKey(key)) continue;
      if (value === null || value === undefined || String(value).trim() === '') continue;
      if (isPlaceholder(value)) continue;
      warnings.push(`Hardcoded secret-like value for ${key}: ${maskValue(String(value))}`);
    }
  }

  const unknownKeysMode = config.checks?.unknownKeys ?? 'off';
  if (unknownKeysMode === 'warn' && Array.isArray(config.knownKeys) && config.knownKeys.length > 0) {
    const unknownKeys = Object.keys(flatData).filter((key) => !config.knownKeys.includes(key));
    if (unknownKeys.length > 0) {
      warnings.push(`Unknown keys detected: ${unknownKeys.slice(0, 10).join(', ')}`);
    }
  }

  const deprecatedKeysMode = config.checks?.deprecatedKeys ?? 'off';
  if (deprecatedKeysMode === 'warn' && Array.isArray(config.deprecatedKeys) && config.deprecatedKeys.length > 0) {
    const deprecated = Object.keys(flatData).filter((key) => config.deprecatedKeys.includes(key));
    if (deprecated.length > 0) {
      warnings.push(`Deprecated keys detected: ${deprecated.slice(0, 10).join(', ')}`);
    }
  }

  return { blockers, warnings, context: { file: path.basename(filePath) } };
}
