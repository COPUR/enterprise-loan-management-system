# MongoDB BCNF/DKNF Baseline (Open Finance Analytics)

## Scope
This baseline applies to MongoDB collections used by the Open Finance analytics silver copy:

- `customer_patterns`
- `consent_metrics_summary`
- `compliance_reports`
- `security_incidents`
- `usage_analytics`

## BCNF Rules (Key-Centric)
Each collection must have one clear business key that determines all non-key facts.

1. `customer_patterns`
- Business key: `customerId` (masked)
- Constraint: unique index on `customerId`
- Rule: one document per customer pattern aggregate

2. `consent_metrics_summary`
- Business key: `participantId`
- Constraint: unique index on `participantId`
- Rule: one participant summary document

3. `compliance_reports`
- Business key: `(reportDate, reportType)`
- Constraint: unique compound index on `reportDate + reportType`
- Rule: one compliance report per date/type pair

## DKNF Rules (Domain-Centric)
Field constraints should be captured by key constraints and explicit domain types.

1. `security_incidents`
- `severity` must be enum: `HIGH|MEDIUM|LOW`
- `status` must be enum: `OPEN|INVESTIGATING|RESOLVED|CLOSED`
- `details` must be typed as `Map<String, String>`

2. `compliance_reports`
- `reportType` must be enum: `DAILY_COMPLIANCE|WEEKLY_SUMMARY|MONTHLY_SUMMARY`
- `status` must be enum: `GENERATED|REVIEWED|SUBMITTED`
- `regulatoryChecks` must be typed as `Map<String, Boolean>`

## Controlled Denormalization
MongoDB denormalization is allowed only when write paths enforce consistency.

1. `customer_patterns`
- Persist canonical fields only (`participantConsents`, `recentScopes`, counters, dates)
- Treat `preferredParticipant` and `frequentScopes` as derived read-model fields (not persisted)

2. `usage_analytics`
- `date` is derived from `timestamp`
- Every write must populate both from the same event timestamp

## Write-Path Invariants
Service logic must preserve these invariants:

1. Customer pattern lookups must use masked `customerId` (same format as persisted key).
2. Security-incident severity filters must validate against enum domain.
3. Compliance violation extraction must be null-safe for optional incident details.
4. Consent metrics summaries returned from aggregation must be re-keyed with request participant.

## Review Checklist (PR Gate)
Before merging analytics changes:

1. Key uniqueness is preserved or explicitly migrated.
2. New string fields with finite values are modeled as enums.
3. New map fields use typed values instead of `Object`.
4. Any derived fields are either transient or write-synchronized.

## Operational Migration
Use the migration scripts to enforce key constraints safely on existing data.

1. Dry-run preflight checks:
`/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/scripts/mongodb/migrate-open-finance-analytics.sh`

2. Apply indexes after preflight passes:
`/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/scripts/mongodb/migrate-open-finance-analytics.sh --apply`

3. Override connection settings when needed:
`MONGO_URI='mongodb://localhost:27017' MONGO_DB='open_finance' /Users/alicopur/Documents/GitHub/enterprise-loan-management-system/scripts/mongodb/migrate-open-finance-analytics.sh --apply`

4. Run static design guardrail checks:
`/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/scripts/validation/validate-mongodb-analytics-design.sh`
