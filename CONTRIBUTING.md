# Contributing

## Prerequisites

- Java 23
- Gradle Wrapper (`./gradlew`)
- GitHub account with repository access

## Development Flow

1. Sync from `main`.
2. Create a branch with prefix `codex/`:
   - `git checkout -b codex/<short-topic>`
3. Keep changes scoped and atomic.
4. Run local quality gates:
   - `./gradlew --no-daemon check`
5. Open a pull request using `.github/pull_request_template.md`.

## Pull Request Requirements

- Linked issue or clear work item.
- All CI checks passing.
- Tests added/updated for behavior changes.
- No secrets or PII in code, logs, fixtures, or screenshots.
- CODEOWNERS review for owned paths.
- Repository structure policy followed:
  - `docs/architecture/REPOSITORY_STRUCTURE_POLICY.md`
  - `docs/architecture/MODULE_OWNERSHIP_MAP.md`

## Repository Governance Rules

1. Frozen paths by default:
   - `archive/`
   - `temp-src/`
   - `simple-test/`
2. Deprecated roots are documentation-only by default:
   - `bankwide/`
   - `bank-wide-services/`
   - `loan-service/`
   - `payment-service/`
3. Changes in `shared-kernel/` or `shared-infrastructure/` require ADR/decision updates.
4. Structural changes must update:
   - `DOCUMENTATION_INDEX.md`
   - Root `readme.md`
   - Relevant architecture docs
5. CI enforces baseline repository governance checks via:
   - `tools/validation/validate-repo-governance.sh`

## Commit Message Style

Use Conventional Commits:

- `feat: ...`
- `fix: ...`
- `refactor: ...`
- `docs: ...`
- `build: ...`
- `test: ...`
- `ci: ...`

## Security and Responsible Disclosure

- Do not open public issues for vulnerabilities.
- Follow `.github/SECURITY.md` for private disclosure.
