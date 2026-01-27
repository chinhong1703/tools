import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { detectConfigFiles } from '../scripts/detect-config-files.mjs';

const repoRoot = '/repo';

test('detects config files by extension and includePaths', () => {
  const files = [
    path.join(repoRoot, 'config/app.yaml'),
    path.join(repoRoot, 'src/app.js'),
    path.join(repoRoot, 'application-prod.properties')
  ];
  const config = { includePaths: ['config'], fileGlobs: [] };
  const result = detectConfigFiles({ files, config, repoRoot });
  assert.deepEqual(result, [path.join(repoRoot, 'config/app.yaml'), path.join(repoRoot, 'application-prod.properties')]);
});

test('detects config files by glob', () => {
  const files = [path.join(repoRoot, 'infra/prod/settings.json')];
  const config = { includePaths: [], fileGlobs: ['infra/**/*.json'] };
  const result = detectConfigFiles({ files, config, repoRoot });
  assert.deepEqual(result, [path.join(repoRoot, 'infra/prod/settings.json')]);
});
