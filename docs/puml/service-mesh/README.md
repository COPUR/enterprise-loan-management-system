# Service Mesh Migration Diagrams

This folder contains architecture views for service-mesh migration planning.

## Artifacts

- `as-is-open-finance-runtime.puml`
- `as-is-open-finance-runtime.png`
- `as-is-open-finance-runtime.svg`
- `to-be-open-finance-service-mesh.puml`
- `to-be-open-finance-service-mesh.png`
- `to-be-open-finance-service-mesh.svg`

## Notes

- **As-Is** shows current runtime with API gateway and direct east-west service calls.
- **To-Be** shows target state with mesh ingress, sidecars, strict mTLS, policy enforcement, and unified telemetry.
- Source of truth is the `.puml` files; image artifacts are generated from those sources.
