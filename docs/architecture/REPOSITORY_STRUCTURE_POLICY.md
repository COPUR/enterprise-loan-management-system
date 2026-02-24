# Repository Structure Policy

## Purpose

Define mandatory structure and change-control rules for this monorepo to keep domain boundaries, delivery governance, and long-term maintainability aligned with DDD, Hexagonal Architecture, and 12-factor delivery practices.

## Canonical Top-Level Areas

1. Domain contexts: `*-context/`
2. Deployable services: `services/`
3. Shared foundations: `shared-kernel/`, `shared-infrastructure/`, `common/`
4. Architecture and governance docs: `docs/`
5. Build and delivery controls: `.github/`, `buildSrc/`, `config/`
6. Tooling and test assets: `tools/`, `postman/`

## Restricted / Legacy Areas

These paths are frozen by default and must not receive new changes without explicit architecture approval and migration rationale in the PR:

1. `archive/`
2. `temp-src/`
3. `simple-test/`

## Deprecated Roots (Wave B Rationalization)

These roots are deprecated and accept documentation-only updates (`README.md`) by default. Functional code changes require explicit migration approval:

1. `bankwide/`
2. `bank-wide-services/`
3. `loan-service/`
4. `payment-service/`

Deletion of legacy files in these roots is allowed as part of approved cleanup/migration work.

## Shared-Foundation Change Rules

Changes under `shared-kernel/` or `shared-infrastructure/` require:

1. Architecture-owner review (CODEOWNERS enforced).
2. Impact statement in PR notes.
3. Backward-compatibility and migration notes.
4. Updated tests in affected consumer modules.

## Structure Change Rules

For new top-level folders or major relocations:

1. Add/update architecture documentation in `docs/architecture/`.
2. Update `DOCUMENTATION_INDEX.md` and root `readme.md`.
3. Update module ownership mapping (`docs/architecture/MODULE_OWNERSHIP_MAP.md`).
4. Add migration guidance and rollback notes in the PR.

## Naming Conventions

1. Prefer lowercase kebab-case for folders.
2. Keep one authoritative location per concern.
3. Avoid duplicate semantic roots (for example, two different folders representing the same platform domain).

## Enforcement

This policy is enforced by:

1. PR checklist in `.github/pull_request_template.md`.
2. CODEOWNERS review requirements.
3. CI governance validation script:
   - `tools/validation/validate-repo-governance.sh`
