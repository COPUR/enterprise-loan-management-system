# PMD Enhancement Review (Hexagonal + BIAN Nomenclature)

## Scope Reviewed
- Repository markdown documents reviewed: `104`
- Review focus:
  - Hexagonal architecture guardrails
  - Domain/Application/Infrastructure boundaries
  - Port/Adapter conventions
  - BIAN-aligned banking nomenclature
  - BCNF/DKNF data design references (for consistency context)

## Review Method
1. Enumerated all `*.md` files in the repository.
2. Parsed architecture-relevant keywords across all markdown files.
3. Prioritized canonical guardrail documents for enforceable static rules.

## Primary Source Documents Used
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/HEXAGONAL_ARCHITECTURE_GUARDRAILS.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/architecture/ARCHITECTURE_GUARDRAILS.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/scripts/README.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/readme.md`
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/docs/architecture/MONGODB_BCNF_DKNF_BASELINE.md`

## Guardrails Derived From Documentation
- Domain layer must be framework/infrastructure independent.
- Application layer should depend on domain ports/contracts, not infrastructure adapters.
- Infrastructure adapters should be explicitly named and isolated.
- Domain events should use explicit event naming.
- Banking service naming should be clear and bounded-context aligned (BIAN-like semantics).

## PMD Enhancements Implemented
Rules added to:
- `/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/config/pmd/banking-rules.xml`

New custom rules:
1. `HexagonalDomainLayerPurity`
- Blocks framework/infrastructure imports in `*.domain..*` packages.
- Detects imports such as `jakarta.persistence.*`, `javax.persistence.*`, `org.springframework.*`, `org.hibernate.*`, `java.sql.*`, and `*.infrastructure.*`.

2. `HexagonalApplicationBoundary`
- Blocks `*.application..*` package imports from `*.infrastructure.*`.

3. `HexagonalAdapterNaming`
- Enforces adapter-oriented suffixes for types under `*.infrastructure.adapter..*`:
  - `Adapter`, `Controller`, `Repository`, `Client`, `Publisher`, `Listener`, `Mapper`, `Config`

4. `DomainEventSuffixConvention`
- Enforces `Event` suffix for classes under `*.domain.event..*`.

5. `BianStyleServiceNomenclature`
- Enforces service naming suffixes in service packages:
  - `Service`, `ServiceDomain`, `Saga`, `Orchestrator`

## Validation
Executed:
- `gradle pmdMain pmdTest --no-daemon --no-configuration-cache --stacktrace`

Result:
- Build successful with enhanced PMD rules active.

## Notes
- BCNF/DKNF constraints are data-model and write-path concerns; they are documented and validated by scripts, not directly enforceable via Java PMD rules.
- The PMD profile now enforces architectural boundaries and naming semantics without breaking current validated modules.
