import test from 'node:test';
import assert from 'node:assert/strict';
import { inferEnv } from '../scripts/env-infer.mjs';

test('infers env from filename and profile key', () => {
  const config = {
    envLabels: ['dev', 'uat', 'prod'],
    prodFilePatterns: ['*-prod.*'],
    profileKeys: ['spring.profiles.active']
  };
  const result = inferEnv({
    filePath: '/repo/application-prod.yaml',
    flatData: { 'spring.profiles.active': 'uat' },
    config
  });

  assert.equal(result.env, 'uat');
  assert.ok(result.warnings[0].includes('Environment mismatch'));
});

test('infers env from filename when profile key absent', () => {
  const config = {
    envLabels: ['dev', 'uat', 'prod'],
    prodFilePatterns: ['*-prod.*'],
    profileKeys: ['spring.profiles.active']
  };
  const result = inferEnv({
    filePath: '/repo/app-uat.json',
    flatData: {},
    config
  });

  assert.equal(result.env, 'uat');
});
