import test from 'node:test';
import assert from 'node:assert/strict';
import { parseJson } from '../scripts/validators/json-parse.mjs';

test('flags invalid json', () => {
  const result = parseJson('{"a": }');
  assert.ok(result.errors.length > 0);
});
