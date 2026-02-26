# GitHub Issue Creation Guide (From PR Drafts)

## Prerequisites
1. GitHub CLI authenticated:
   ```bash
   gh auth status
   ```
2. Repository default remote available:
   ```bash
   git remote -v
   ```

## 1) Create Milestones
Use milestone names from:
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/github-milestone-map.md`

```bash
gh api repos/COPUR/enterprise-loan-management-system/milestones \
  -f title='MS1-Wave0-Gate-Hardening' \
  -f state='open' \
  -f description='Turn quality gates into blocking checks'

gh api repos/COPUR/enterprise-loan-management-system/milestones \
  -f title='MS2-Wave1-Core-Context-Stabilization' \
  -f state='open' \
  -f description='Lift customer, loan, and payment contexts above test-risk threshold'

gh api repos/COPUR/enterprise-loan-management-system/milestones \
  -f title='MS3-Scorecard-And-Observability-Closure' \
  -f state='open' \
  -f description='Automate scorecard publishing and CI telemetry'
```

## 2) Create Labels (if missing)
```bash
gh label create program:transformation --color 1D76DB --description 'Transformation program work'
gh label create wave:0 --color B60205 --description 'Wave 0 gate hardening'
gh label create wave:1 --color FBCA04 --description 'Wave 1 stabilization'
gh label create wave:closure --color 0E8A16 --description 'Closure and automation'
gh label create type:ci --color 5319E7 --description 'CI/CD related work'
gh label create type:test --color C2E0C6 --description 'Testing work'
gh label create type:contract --color D4C5F9 --description 'API contract and OpenAPI work'
gh label create priority:high --color D93F0B --description 'High priority'
```

## 3) Create Issues From Drafts
Drafts are stored in:
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/github-issue-drafts/`

Example:
```bash
gh issue create \
  --title \"[PR-001] Enforce mandatory coverage gates in root pipeline\" \
  --body-file docs/enterprisearchitecture/implementation-development/transformation/outputs/github-issue-drafts/PR-001.md \
  --assignee devsecops-platform-lead \
  --milestone 'MS1-Wave0-Gate-Hardening' \
  --label program:transformation \
  --label type:ci \
  --label priority:high \
  --label wave:0
```

Repeat for `PR-002` to `PR-014` with matching milestone/labels per draft.

## 4) Link Dependencies
After creation, add dependency references in issue body comments:
```text
Depends on #<issue-number>
```

## 5) Verification
1. Each issue has:
   - assignee
   - milestone
   - labels
   - acceptance criteria checklist
2. Milestone board shows expected issue counts:
   - MS1: 5 issues
   - MS2: 7 issues
   - MS3: 2 issues
