# /config-guard-init

This command runs an interactive setup wizard for Config Guard. It asks project-specific questions and writes a `.config-guard.json` file to the repo root (current working directory). You can re-run it any time to update the configuration.

## Wizard flow (use AskUserQuestion / askquestion)

1) **Config file locations**
   - Ask: "Which directories or path prefixes should be scanned for config files? (comma-separated, e.g., `src/main/resources, config, deploy`)"
   - Default: `.`

2) **Additional glob patterns (optional)**
   - Ask: "Any extra file globs to include? (comma-separated, e.g., `**/*.config.*`, `infra/**/*.yml`)"
   - Default: empty

3) **Prod filename patterns**
   - Ask: "Which filename patterns represent PROD configs?" (show default `[*-prod.* , prod.*]` and allow accept)
   - Default: `[*-prod.* , prod.*]`

4) **Profile key names inside files**
   - Ask: "Which keys indicate the active environment inside configs? (comma-separated)"
   - Default: `spring.profiles.active, spring.config.activate.on-profile, app.env, environment`

5) **Required keys per environment**
   - For each env label: dev / uat / sit / staging / prod
   - Ask: "List required keys for <env> (comma-separated, blank for none)"

6) **UAT-only keys**
   - Ask: "List keys allowed only in UAT (comma-separated, blank for none)"

7) **URL allow/deny lists**
   - For each env label, ask (optional):
     - "Allow-listed hosts for <env>? (comma-separated, blank for none)"
     - "Deny-listed hosts for <env>? (comma-separated, blank for none)"
   - Note: keyword heuristics remain enabled.

8) **Known/deprecated keys**
   - Ask: "Provide a list of known keys (comma-separated) to warn on unknown keys, or leave blank to disable."
   - Ask: "Provide a list of deprecated keys (comma-separated) to warn on deprecated usage, or leave blank to disable."

9) **Write config**
   - Write `.config-guard.json` with the following schema:

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

10) **Finish**
   - Print a short explanation:
     - The Stop hook validates changed config files using this config.
     - It blocks only for blockers (UAT/dev URLs in prod or missing required keys, plus Claude blockers).
     - Re-run `/config-guard-init` to update rules.

## Implementation notes
- Use `AskUserQuestion` / `askquestion` tool for prompts.
- Write `.config-guard.json` to the current working directory.
- Keep output concise and actionable.
