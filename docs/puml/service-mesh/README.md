# Service Mesh Migration Diagrams

This folder contains architecture views for service-mesh migration planning.

## Artifacts

- `as-is-open-finance-runtime.puml`
- `as-is-open-finance-runtime.png`
- `as-is-open-finance-runtime.svg`
- `to-be-open-finance-service-mesh.puml`
- `to-be-open-finance-service-mesh.png`
- `to-be-open-finance-service-mesh.svg`
- `organizational-as-is-big-picture.puml`
- `organizational-as-is-big-picture.png`
- `organizational-as-is-big-picture.svg`
- `organizational-to-be-big-picture.puml`
- `organizational-to-be-big-picture.png`
- `organizational-to-be-big-picture.svg`
- `enterprise-capability-map.puml`
- `enterprise-capability-map.png`
- `enterprise-capability-map.svg`
- `plan.md`
- `refactor.md`
- `keycloak-aaa-blueprint.md`
- `implementation-plan-industrial-standards-tdd-data-examples.md`

## Linked Architecture Documents

- `docs/architecture/ORGANIZATIONAL_BIG_PICTURE_AS_IS_TO_BE.md`
- `docs/architecture/ENTERPRISE_CAPABILITY_MAP.md`
- `docs/architecture/REPOSITORY_CLEAN_CODING_REVIEW.md`
- `docs/architecture/REPOSITORY_STRUCTURE_POLICY.md`
- `docs/architecture/MODULE_OWNERSHIP_MAP.md`
- `docs/GENERAL_BACKLOG.md`

## Notes

- **As-Is** shows current runtime with API gateway and direct east-west service calls.
- **To-Be** shows target state with centralized AAA (Keycloak + LDAP), distributed authz agents, DPoP enforcement, mesh ingress, sidecars, strict mTLS, and unified telemetry.
- **Organizational As-Is/To-Be** adds operating-model and governance transformation views.
- **Enterprise Capability Map** visualizes target capability domains and maturity direction.
- Source of truth is the `.puml` files; image artifacts are generated from those sources.
