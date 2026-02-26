# Data-at-Rest Policy

## Mandatory Controls

1. Real credentials and API keys must never be stored in:
   - source code,
   - local `.env`/`.env.local` files,
   - OpenAPI examples or test fixtures.
2. Runtime key provisioning must use internal control-plane API:
   - `POST /internal/v1/system/secrets`
3. Metadata APIs must never return plaintext key values.
4. Persist only protected representations:
   - masked display value,
   - salted cryptographic hash,
   - versioned metadata.

## Runtime API Contract

- `POST /internal/v1/system/secrets`
  - Stores/rotates key material through authenticated internal channel.
  - Response includes masked value and metadata only.
- `GET /internal/v1/system/secrets/{secretKey}`
  - Returns metadata only.
  - Plaintext key value is not retrievable.

## Data-at-Rest Principles

1. Enable encryption at rest for database volumes and backups/snapshots.
2. Apply least-privilege DB access for key metadata tables.
3. Keep immutable audit trail for key mutation events (actor, key, version, timestamp).
4. Enforce periodic rotation and version increments.
5. Do not log plaintext key material in application logs.

## Implementation Note

The consent authorization service implements this policy through database-backed
runtime key storage using masked + hash/salt fields and metadata-only retrieval.
