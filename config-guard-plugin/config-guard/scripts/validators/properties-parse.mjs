export function parseProperties(content) {
  const data = {};
  const warnings = [];
  const errors = [];
  const seenKeys = new Set();

  const lines = content.split(/\r?\n/);
  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) return;

    const separatorIndex = trimmed.search(/[:=]/);
    if (separatorIndex === -1) {
      errors.push(`Line ${index + 1}: missing key/value separator`);
      return;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    const value = trimmed.slice(separatorIndex + 1).trim();

    if (!key) {
      errors.push(`Line ${index + 1}: empty key`);
      return;
    }

    if (seenKeys.has(key)) {
      warnings.push(`Duplicate key "${key}" on line ${index + 1}`);
    }
    seenKeys.add(key);
    data[key] = value;
  });

  return { data, warnings, errors };
}
