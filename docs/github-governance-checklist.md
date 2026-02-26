# GitHub Governance Checklist (Aligned with GitHub Docs)

This repository now contains standards files and workflows, but some controls must still be set in GitHub repository settings.

## Repository files now in place

- `.github/CODEOWNERS`
- `.github/SECURITY.md`
- `.github/SUPPORT.md`
- `.github/dependabot.yml`
- `.github/release.yml`
- `.github/pull_request_template.md`
- `.github/ISSUE_TEMPLATE/*`
- Workflows:
  - `.github/workflows/ci.yml`
  - `.github/workflows/codeql.yml`
  - `.github/workflows/release.yml`

## Required repository settings (manual in GitHub UI)

1. Protect `main`:
   - Require pull request before merging
   - Require approvals
   - Dismiss stale approvals on new commits
   - Require conversation resolution
   - Require status checks (CI, CodeQL, dependency review)
   - Restrict force-push and deletion
   - Require linear history (recommended)
   - Require deployments to succeed before merge (if environments are configured)
2. Protect `develop` with the same policy (if used as integration branch).
3. Configure a repository ruleset for `main`/`develop` to enforce:
   - PR-only merges
   - Required status checks
   - Code scanning enabled
   - Restriction of bypass permissions to repository administrators only
4. Enable:
   - Dependabot alerts
   - Dependabot security updates
   - Secret scanning
   - Push protection for secrets
   - Private vulnerability reporting
5. Ensure default branch is `main`.
6. Enforce signed commits (optional but recommended for regulated environments).

## Required status checks to add in branch protection/rulesets

1. `Build and Test`
2. `Dependency Review`
3. `Analyze` (CodeQL)

## Branch and PR standards

1. Require conventional commits for merge readiness.
2. Require linked issue in PR template completion.
3. Require CODEOWNERS review for protected paths.

## Recommended labels to create

- `bug`
- `enhancement`
- `security`
- `documentation`
- `ci`
- `breaking-change`
- `skip-changelog`

## Operational cadence

1. Review Dependabot PRs weekly.
2. Review CodeQL alerts daily.
3. Review security advisories and triage within 2 business days.

## GitHub docs references

- Managing branch protection rules: https://docs.github.com/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches
- About rulesets: https://docs.github.com/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets
- Security policy file: https://docs.github.com/code-security/getting-started/adding-a-security-policy-to-your-repository
- Code owners: https://docs.github.com/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- Dependabot configuration: https://docs.github.com/code-security/dependabot/working-with-dependabot/dependabot-options-reference
- Code scanning with CodeQL: https://docs.github.com/code-security/code-scanning/introduction-to-code-scanning/about-code-scanning-with-codeql
- Dependency review action: https://github.com/actions/dependency-review-action
- About issue and PR templates: https://docs.github.com/communities/using-templates-to-encourage-useful-issues-and-pull-requests
