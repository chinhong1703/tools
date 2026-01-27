import path from 'node:path';

const CONFIG_EXTENSIONS = new Set(['.properties', '.yml', '.yaml', '.json']);

function normalizePath(value) {
  return value.split(path.sep).join('/');
}

export function globToRegExp(glob) {
  const escaped = glob
    .replace(/[.+^${}()|[\]\\]/g, '\\$&')
    .replace(/\*\*/g, '<<<DOUBLE_STAR>>>')
    .replace(/\*/g, '[^/]*')
    .replace(/\?+/g, '.')
    .replace(/<<<DOUBLE_STAR>>>/g, '.*');
  return new RegExp(`^${escaped}$`);
}

function matchesGlob(glob, filePath) {
  const regex = globToRegExp(glob);
  return regex.test(filePath);
}

function matchesIncludePaths(includePaths, relativePath) {
  if (!includePaths || includePaths.length === 0) return false;
  if (includePaths.includes('.')) return true;
  return includePaths.some((prefix) => {
    const normalized = normalizePath(prefix.replace(/\/$/, ''));
    return relativePath === normalized || relativePath.startsWith(`${normalized}/`);
  });
}

export function detectConfigFiles({ files, config, repoRoot }) {
  const includePaths = config.includePaths ?? ['.'];
  const fileGlobs = config.fileGlobs ?? [];

  return files.filter((file) => {
    const ext = path.extname(file).toLowerCase();
    if (!CONFIG_EXTENSIONS.has(ext)) return false;

    const relativePath = normalizePath(path.relative(repoRoot, file));
    const baseName = path.basename(relativePath);

    const inIncludePath = matchesIncludePaths(includePaths, relativePath);
    const inGlob = fileGlobs.some((glob) => matchesGlob(normalizePath(glob), relativePath));
    const isSpringDefault = /^application.*\.(properties|ya?ml|json)$/i.test(baseName);

    return inIncludePath || inGlob || isSpringDefault;
  });
}
