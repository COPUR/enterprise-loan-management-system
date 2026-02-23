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
