import test from 'node:test';
import assert from 'node:assert/strict';
import { maskSensitiveValues } from '../scripts/claude-review.mjs';

test('masks sensitive values in prompt content', () => {
  const content = 'password=supersecret\napiKey: abc123';
  const masked = maskSensitiveValues(content);
  assert.ok(masked.includes('***MASKED***'));
});
