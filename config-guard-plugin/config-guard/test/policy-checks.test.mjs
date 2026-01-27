import test from 'node:test';
import assert from 'node:assert/strict';
import { collectMissingRequiredKeys, runPolicyChecks } from '../scripts/validators/policy-checks.mjs';

test('blocks prod urls with uat keywords', () => {
  const result = runPolicyChecks({
    filePath: '/repo/application-prod.yaml',
    env: 'prod',
    flatData: {},
    content: 'endpoint: https://uat.example.com/api',
    config: {
      urlRules: { keywordHeuristics: true, keywordsByEnv: { uat: ['uat'] } },
      checks: { debugInProd: 'warn', hardcodedSecrets: 'warn' }
    }
  });

  assert.ok(result.blockers.some((issue) => issue.includes('non-prod')));
});

test('flags missing required keys', () => {
  const missing = collectMissingRequiredKeys('prod', { 'app.name': 'x' }, { requiredKeys: { prod: ['app.name', 'app.url'] } });
  assert.deepEqual(missing, ['Missing required key: app.url']);
});
