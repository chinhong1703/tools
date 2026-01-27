import path from 'node:path';
import { globToRegExp } from './detect-config-files.mjs';

function matchesPattern(pattern, fileName) {
  const regex = globToRegExp(pattern);
  return regex.test(fileName);
}

function inferFromFilename(fileName, envLabels, prodPatterns) {
  const envFromFile = new Map();
  for (const env of envLabels) {
    const patterns = env === 'prod'
      ? prodPatterns
      : [`*-${env}.*`, `${env}.*`];
    for (const pattern of patterns) {
      if (matchesPattern(pattern, fileName)) {
        envFromFile.set(env, pattern);
        break;
      }
    }
  }
  return envFromFile.size ? Array.from(envFromFile.keys())[0] : null;
}

function inferFromProfileKeys(flatData, envLabels, profileKeys) {
  for (const key of profileKeys) {
    const value = flatData[key];
    if (typeof value !== 'string') continue;
    const tokens = value.split(/[\s,]+/).map((token) => token.trim()).filter(Boolean);
    for (const env of envLabels) {
      if (tokens.includes(env)) {
        return env;
      }
    }
  }
  return null;
}

export function inferEnv({ filePath, flatData, config }) {
  const envLabels = config.envLabels ?? ['dev', 'uat', 'sit', 'staging', 'prod'];
  const prodFilePatterns = config.prodFilePatterns ?? ['*-prod.*', 'prod.*'];
  const profileKeys = config.profileKeys ?? [];
  const fileName = path.basename(filePath);

  const fileEnv = inferFromFilename(fileName, envLabels, prodFilePatterns);
  const profileEnv = inferFromProfileKeys(flatData, envLabels, profileKeys);

  const warnings = [];
  let env = fileEnv || profileEnv || null;

  if (profileEnv && fileEnv && profileEnv !== fileEnv) {
    warnings.push(`Environment mismatch: filename suggests ${fileEnv} but profile keys suggest ${profileEnv}.`);
    env = profileEnv;
  } else if (profileEnv) {
    env = profileEnv;
  }

  return { env, warnings, source: profileEnv ? 'profile' : 'filename' };
}
