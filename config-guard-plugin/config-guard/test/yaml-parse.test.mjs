import test from 'node:test';
import assert from 'node:assert/strict';
import { parseYaml } from '../scripts/validators/yaml-parse.mjs';

test('flags invalid yaml', () => {
  const result = parseYaml('key: [unclosed');
  assert.ok(result.errors.length > 0);
});
