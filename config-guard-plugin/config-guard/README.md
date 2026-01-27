# Config Guard (Claude Code Plugin)

Config Guard enforces configuration-file correctness using a Stop hook and an interactive setup wizard.

## Quick start

```bash
claude --plugin-dir ./config-guard-plugin/config-guard
```

Run the setup wizard:

```
/config-guard:config-guard-init
```

This creates `.config-guard.json` in the repo root.

## How the Stop hook works

- The Stop hook checks **modified + staged** config files (`.properties`, `.yml/.yaml`, `.json`).
- It performs deterministic validation, policy checks, and optionally a semantic review using the local `claude` CLI.
- **Blockers** block stop with a concise reason:
  - UAT/dev/sit/staging URLs in prod configs
  - Missing required keys
  - UAT keys missing from PROD (when PROD files are changed, except for `uatOnlyKeys`)
- **Warnings** do not block stop (but are summarized if blockers occur).
- If not in a Git repo, the hook is a no-op.

## Configuration schema (`.config-guard.json`)

```json
{
  "version": 1,
  "includePaths": ["."],
  "fileGlobs": [],
  "prodFilePatterns": ["*-prod.*", "prod.*"],
  "envLabels": ["dev", "uat", "sit", "staging", "prod"],
  "profileKeys": ["spring.profiles.active", "spring.config.activate.on-profile", "app.env", "environment"],
  "requiredKeys": {
    "dev": [],
    "uat": [],
    "sit": [],
    "staging": [],
    "prod": []
  },
  "uatImpliesProdKeys": true,
  "uatOnlyKeys": [],
  "urlRules": {
    "keywordHeuristics": true,
    "keywordsByEnv": {
      "dev": ["dev"],
      "uat": ["uat"],
      "sit": ["sit"],
      "staging": ["staging", "stage"],
      "prod": ["prod", "production"]
    },
    "allowHostsByEnv": {},
    "denyHostsByEnv": {}
  },
  "checks": {
    "debugInProd": "warn",
    "hardcodedSecrets": "warn",
    "unknownKeys": "off",
    "deprecatedKeys": "off"
  },
  "knownKeys": [],
  "deprecatedKeys": []
}
```

## Privacy and safety

- Runs entirely locally via the `claude` CLI.
- Sensitive values are best-effort masked before being sent to Claude for review.
- No bypass: if blockers are detected, stop is blocked.

## Tests

```bash
npm test
```
