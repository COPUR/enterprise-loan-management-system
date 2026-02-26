# Wave B Legacy Root Rationalization Plan

## Purpose

Reduce structural drift by deprecating duplicate or non-canonical roots and routing future work to bounded contexts and service runtime modules.

## Scope

1. `bankwide/`
2. `bank-wide-services/`
3. `loan-service/`
4. `payment-service/`

## Current State

1. `bankwide/` contains a legacy `build.gradle`.
2. `bank-wide-services/`, `loan-service/`, and `payment-service/` mostly contain residual build outputs or empty shells.
3. Active implementation has moved to bounded contexts and `services/`.

## Reference Inventory (Executed)

1. No active Gradle module include dependencies found for deprecated roots in `settings.gradle`.
2. Existing occurrences of `loan-service` and `payment-service` are primarily logical service names in documentation, Postman assets, and architecture examples.
3. Residual tracked file cleanup completed:
   - Removed `bankwide/build.gradle`.
4. Governance validator now:
   - Blocks reintroduction of deprecated roots into `settings.gradle`.
   - Warns on residual tracked files in deprecated roots (can be escalated to hard-fail via `STRICT_DEPRECATED_ROOTS=true`).

## Target State

1. No new feature development in deprecated roots.
2. Canonical locations:
   - Domain logic: `*-context/`
   - Deployable services: `services/`
   - Shared cross-cutting code: `shared-kernel/`, `shared-infrastructure/`, `common/`
3. Deprecated roots retained only as temporary compatibility placeholders until explicit archival/removal.

## Enforcement Strategy

1. Governance validator blocks non-documentation edits in deprecated roots.
2. Per-folder deprecation `README.md` files explain migration destinations.
3. Any exceptional change requires:
   - Architecture approval
   - Migration rationale in PR notes
   - Follow-up removal/relocation task

## Migration Steps

## Step 1 (Completed in Wave B kickoff)

1. Add deprecation markers to all legacy roots.
2. Add CI governance checks to block unauthorized edits.

## Step 2 (Planned)

1. Inventory any remaining references from build scripts/docs.
2. Relocate residual logic to canonical modules where needed.
3. Validate no regression from deprecated-root cleanup in CI.

## Step 3 (Planned)

1. Archive or remove deprecated roots after two release cycles with no active dependencies.
2. Update architecture docs and ownership map accordingly.
