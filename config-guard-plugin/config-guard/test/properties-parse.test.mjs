import test from 'node:test';
import assert from 'node:assert/strict';
import { parseProperties } from '../scripts/validators/properties-parse.mjs';

test('flags malformed properties lines', () => {
  const result = parseProperties('valid=value\ninvalidline');
  assert.equal(result.errors.length, 1);
});
