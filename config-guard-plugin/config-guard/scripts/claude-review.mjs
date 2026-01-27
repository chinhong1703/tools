import { spawn } from 'node:child_process';

let cachedFlags = null;

async function detectClaudeFlags() {
  if (cachedFlags) return cachedFlags;
  return new Promise((resolve) => {
    const child = spawn('claude', ['--help']);
    let output = '';
    child.stdout.on('data', (chunk) => {
      output += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      output += chunk.toString();
    });
    child.on('close', () => {
      const flags = {
        print: output.includes('--print'),
        stdin: output.includes('--stdin'),
        json: output.includes('--json')
      };
      cachedFlags = flags;
      resolve(flags);
    });
    child.on('error', () => {
      cachedFlags = { print: false, stdin: false, json: false };
      resolve(cachedFlags);
    });
  });
}

function maskSensitiveValues(content) {
  return content.replace(/(password|secret|token|apikey|api[_-]?key|credential|key)\s*[:=]\s*[^\n\r]+/gi, (match) => {
    const [keyPart] = match.split(/[:=]/);
    return `${keyPart}: ***MASKED***`;
  });
}

function buildPrompt({ filePath, env, content }) {
  return [
    'You are reviewing a configuration file for correctness and policy compliance.',
    'Return strict JSON only, no markdown.',
    'Output either {"ok": true} or {"ok": false, "issues": [{"severity":"blocker"|"warn","message":"...","suggested_fix":"..."}]}',
    `File: ${filePath}`,
    `Inferred environment: ${env ?? 'unknown'}`,
    'Check for:',
    '- syntax or formatting mistakes missed by parser',
    '- environment mismatches (uat/dev endpoints in prod)',
    '- suspicious profile activation keys',
    '- inconsistent or likely-typo keys',
    '- credentials/secrets handling',
    'Full file content follows:',
    '---',
    content,
    '---'
  ].join('\n');
}

function parseClaudeResponse(output) {
  try {
    const parsed = JSON.parse(output.trim());
    if (parsed.ok === true) return { issues: [] };
    if (parsed.ok === false && Array.isArray(parsed.issues)) {
      return { issues: parsed.issues };
    }
    return { issues: [], warning: 'Claude response schema mismatch.' };
  } catch (error) {
    return { issues: [], warning: `Claude response parse error: ${error.message}` };
  }
}

export async function reviewFileWithClaude({ filePath, env, content }) {
  const flags = await detectClaudeFlags();
  const maskedContent = maskSensitiveValues(content);
  const prompt = buildPrompt({ filePath, env, content: maskedContent });

  return new Promise((resolve) => {
    const args = [];
    if (flags.print) args.push('--print');
    if (flags.stdin) args.push('--stdin');
    const child = spawn('claude', args);
    let output = '';
    let errorOutput = '';

    child.stdout.on('data', (chunk) => {
      output += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      errorOutput += chunk.toString();
    });
    child.on('close', () => {
      if (!output && errorOutput) {
        resolve({ issues: [], warning: 'Claude CLI produced no output.' });
        return;
      }
      const parsed = parseClaudeResponse(output);
      resolve(parsed);
    });
    child.on('error', (error) => {
      resolve({ issues: [], warning: `Claude CLI error: ${error.message}` });
    });

    child.stdin.write(prompt);
    child.stdin.end();
  });
}

export { maskSensitiveValues };
