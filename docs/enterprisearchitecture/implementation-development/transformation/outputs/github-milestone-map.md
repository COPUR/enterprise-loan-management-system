# GitHub Milestone Map (PR-Ready Transformation Batch)

## Milestones
| Milestone | Goal | Planned Start | Planned End | Included PR IDs | Exit Criteria |
| --- | --- | --- | --- | --- | --- |
| `MS1-Wave0-Gate-Hardening` | Turn quality gates into blocking checks | 2026-02-25 | 2026-03-10 | PR-001, PR-002, PR-003, PR-004, PR-011 | Coverage gate enforced; architecture boundary tests active; OpenAPI lint gate active |
| `MS2-Wave1-Core-Context-Stabilization` | Lift customer, loan, payment contexts above test-risk threshold | 2026-03-11 | 2026-03-31 | PR-005, PR-006, PR-007, PR-008, PR-009, PR-010, PR-012 | Contexts no longer `needs-tests`; integration and contract tests pass |
| `MS3-Scorecard-And-Observability-Closure` | Automate scorecard publishing and test telemetry visibility | 2026-04-01 | 2026-04-07 | PR-013, PR-014 | Scorecard regenerates on merge; CI telemetry summary available on PR checks |

## Assignee Recommendations By Team
| Team | Suggested Assignee Placeholder |
| --- | --- |
| DevSecOps Platform Team | `@devsecops-platform-lead` |
| Architecture Enablement Team | `@architecture-enablement-lead` |
| Customer Domain Team | `@customer-domain-lead` |
| Loan Domain Team | `@loan-domain-lead` |
| Payment Domain Team | `@payment-domain-lead` |
| API Standards Team | `@api-standards-lead` |
| SRE Observability Team | `@sre-observability-lead` |

## Label Taxonomy
- `type:engineering`
- `type:test`
- `type:contract`
- `type:ci`
- `priority:high`
- `program:transformation`
- `wave:0`
- `wave:1`
- `wave:closure`

## Suggested GitHub Setup Order
1. Create the three milestones.
2. Ensure label taxonomy exists.
3. Create issues from draft files under:
   - `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/enterprisearchitecture/implementation-development/transformation/outputs/github-issue-drafts/`
4. Assign by team lead placeholders or mapped usernames.
5. Add dependency links between issues using `Depends on #<issue-number>`.
