# Party Management Classes - Archived

**Package**: `com.banking.loans.domain.party.*`  
**Archive Date**: 2024-12  
**Risk Level**: Low (Isolated experimental code)  
**Status**: Safely archived - No external dependencies found  

## Archived Classes

- `ComplianceLevel.java` - Compliance level enumeration
- `GroupRole.java` - Party group role definitions  
- `GroupType.java` - Group type classifications
- `Party.java` - Base party domain model
- `PartyGroup.java` - Party group aggregate
- `PartyRole.java` - Party role assignments
- `PartyStatus.java` - Party status enumeration
- `PartyType.java` - Party type classifications
- `RoleSource.java` - Role source definitions

## Analysis Results

- ✅ **No external dependencies** found in main application
- ✅ **Not imported** by any `com.bank.*` classes
- ✅ **Not configured** in Spring Boot component scanning
- ✅ **No REST endpoints** using these classes
- ✅ **No database entities** mapped to these classes

## Business Context

These classes appear to implement a **Party Management** domain following BIAN (Banking Industry Architecture Network) standards. However, the current Enterprise Loan Management System uses a simpler Customer-focused domain model.

## Recovery Instructions

If Party Management needs to be restored:

1. **Copy classes** from this archive to `src/main/java/com/bank/loanmanagement/party/domain/`
2. **Update package declarations** to match the main namespace
3. **Add Spring Boot component scanning** if needed
4. **Create JPA entities** if persistence is required
5. **Add REST controllers** if API access is needed

This functionality could be valuable for future **multi-entity banking** or **corporate banking** features.