# Consent and Authorization Service

DDD/Hexagonal implementation for consent and authorization, including lifecycle transitions and scope validation.

- Runtime: Java 23 + Gradle
- Architecture: Hexagonal (Ports & Adapters)
- Tests: Unit + Integration + E2E/UAT (RestAssured)

## PKCE + SoftHSM Security Layer

The service includes Authorization Code Flow with PKCE endpoints:

- `GET /oauth2/authorize`
- `POST /oauth2/token`

Security implementation details:

- PKCE challenge method: `S256`
- SHA-256 hash provider: SoftHSM PKCS#11 (`SunPKCS11`) when enabled
- SoftHSM library default: `/opt/softhsm2-devel/lib/softhsm/libsofthsm2.so`
- Token label metadata default: `first token`
- Fallback to JDK SHA-256 is configurable

Configuration is in `/src/main/resources/application.yml` under:

- `openfinance.security.oauth2.*`
- `openfinance.security.softhsm.*`

## Internal JWT Lifecycle Security

The service now includes a private JWT lifecycle API for internal services:

- `POST /internal/v1/authenticate`
- `POST /internal/v1/logout`
- `GET /internal/v1/business`
- `POST /internal/v1/system/secrets`
- `GET /internal/v1/system/secrets/{secretKey}`

Security controls applied:

- Stateless bearer-token security chain for protected internal endpoints.
- Signed JWT with issuer/audience validation, `jti`, `iat`, and `exp` checks.
- Token session state with revoke/deactivate semantics (logout and token rotation).
- Brute-force mitigation via failed-login throttling and temporary lockout.
- Standardized JSON error responses for unauthorized/rate-limited access.
- Configuration externalized via environment variables (`12-factor` alignment).

Internal OpenAPI contract:

- `/src/main/resources/openapi/internal-jwt-lifecycle-service.yaml`

## Credential Handling and Data-at-Rest

- Do not store real credentials in local `.env` files or source.
- Provision runtime credentials through `POST /internal/v1/system/secrets`.
- Persistence stores masked value and salted hash metadata only.
- Apply database encryption-at-rest and restricted access for credential tables.

Reference policy:

- `/docs/architecture/DATA_AT_REST_POLICY.md`
