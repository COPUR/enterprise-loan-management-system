# Module Ownership Map

## Purpose

Provide a single ownership reference for bounded contexts, shared foundations, platform controls, and governance-critical repository paths.

## Domain Product Streams

| Area | Scope | Primary Owner | Supporting Owner |
| --- | --- | --- | --- |
| `open-finance-context/` | Consent, AIS, CoP, Open Finance integration | Open Finance Stream | Security Platform |
| `loan-context/` | Loan lifecycle, origination, servicing | Lending Stream | Risk + Compliance |
| `payment-context/` | Payment initiation and settlement | Payments Stream | Security Platform |
| `customer-context/` | Customer profile and onboarding core | Customer Stream | Compliance Stream |
| `risk-context/` | Risk scoring and risk policies | Risk Stream | Lending + Payments |
| `compliance-context/` | Regulatory controls and evidence domain logic | Compliance Stream | Security Council |

## Shared Foundations

| Area | Scope | Primary Owner | Supporting Owner |
| --- | --- | --- | --- |
| `shared-kernel/` | Domain primitives and shared contracts | Architecture Board | Domain Streams |
| `shared-infrastructure/` | Common infrastructure adapters | Platform Engineering | Security + SRE |
| `common/` | Shared cross-cutting utilities | Platform Engineering | Architecture Board |

## Service Runtime and Platforms

| Area | Scope | Primary Owner | Supporting Owner |
| --- | --- | --- | --- |
| `services/` | Deployable service implementations | Domain Streams | Platform Engineering |
| `amanahfi-platform/` | Islamic banking platform modules | AmanahFi Stream | Compliance Stream |
| `masrufi-framework/` | Islamic finance framework modules | Masrufi Stream | Architecture Board |

## Governance and Delivery Controls

| Area | Scope | Primary Owner | Supporting Owner |
| --- | --- | --- | --- |
| `.github/` | CI/CD, templates, governance automation | DevSecOps Platform | Architecture Board |
| `config/` | Lint, static analysis, rule configuration | DevSecOps Platform | Architecture Board |
| `buildSrc/` | Shared build plugins and conventions | Build Platform | DevSecOps Platform |
| `docs/` | Architecture and operational documentation | Architecture Board | All Streams |

## Restricted / Legacy Areas

| Area | Scope | Owner | Policy |
| --- | --- | --- | --- |
| `archive/` | Historical snapshots | Architecture Board | Frozen by default |
| `temp-src/` | Temporary migration workspace | Architecture Board | Frozen by default |
| `simple-test/` | Legacy experimental tests | Architecture Board | Frozen by default |
| `bankwide/` | Deprecated legacy service root | Architecture Board | Documentation-only updates by default |
| `bank-wide-services/` | Deprecated duplicate naming root | Architecture Board | Documentation-only updates by default |
| `loan-service/` | Deprecated legacy service root | Architecture Board | Documentation-only updates by default |
| `payment-service/` | Deprecated legacy service root | Architecture Board | Documentation-only updates by default |

## CODEOWNERS Alignment

All ownership in this document must remain aligned with `.github/CODEOWNERS`. Update both files together in the same change set when ownership boundaries shift.
