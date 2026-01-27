import fs from 'node:fs';
import path from 'node:path';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

const execFileAsync = promisify(execFile);

export async function getRepoRoot(startDir) {
  let current = path.resolve(startDir);
  let last = null;
  while (current && current !== last) {
    const gitPath = path.join(current, '.git');
    if (fs.existsSync(gitPath)) {
      return current;
    }
    last = current;
    current = path.dirname(current);
  }
  return null;
}

export async function getChangedFiles(repoRoot) {
  try {
    const { stdout } = await execFileAsync('git', ['status', '--porcelain=v1'], { cwd: repoRoot });
    if (!stdout.trim()) return [];
    const lines = stdout.split('\n').filter(Boolean);
    const files = new Set();
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.length < 4) continue;
      const status = trimmed.slice(0, 2);
      let filePath = trimmed.slice(3).trim();
      if (status.startsWith('R') && filePath.includes('->')) {
        filePath = filePath.split('->').pop().trim();
      }
      if (filePath) {
        files.add(path.resolve(repoRoot, filePath));
      }
    }
    return Array.from(files);
  } catch {
    return [];
  }
}
