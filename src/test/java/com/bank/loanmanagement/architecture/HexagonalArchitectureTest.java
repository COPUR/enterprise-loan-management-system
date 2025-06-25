package com.bank.loanmanagement.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests to validate Hexagonal Architecture principles
 * Ensures clean separation of concerns and proper dependency direction
 */
@DisplayName("Hexagonal Architecture Validation")
public class HexagonalArchitectureTest {
    
    private JavaClasses importedClasses;
    
    @BeforeEach
    void setUp() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.bank.loanmanagement");
    }
    
    @Test
    @DisplayName("Domain layer should not depend on infrastructure")
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("Domain should not depend on infrastructure concerns");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Domain layer should not depend on Spring Framework")
    void domainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .because("Domain should be framework-agnostic");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Domain layer should not depend on JPA")
    void domainShouldNotDependOnJPA() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .because("Domain should not depend on persistence technology");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Application layer should not depend on infrastructure details")
    void applicationShouldNotDependOnInfrastructureDetails() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..")
            .because("Application layer should only depend on abstractions");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Infrastructure should depend on domain abstractions")
    void infrastructureShouldDependOnDomainAbstractions() {
        ArchRule rule = classes()
            .that().resideInAPackage("..infrastructure.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..domain..")
            .because("Infrastructure adapters should implement domain contracts");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Domain entities should not have JPA annotations")
    void domainEntitiesShouldNotHaveJPAAnnotations() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("jakarta.persistence.Table")
            .orShould().beAnnotatedWith("jakarta.persistence.Column")
            .because("Domain entities should be pure POJOs without infrastructure annotations");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Only infrastructure classes should have JPA annotations")
    void onlyInfrastructureShouldHaveJPAAnnotations() {
        ArchRule rule = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage("..infrastructure.persistence..")
            .because("JPA entities should be in infrastructure layer");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Repository interfaces should be in domain layer")
    void repositoryInterfacesShouldBeInDomain() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should().resideInAPackage("..domain..")
            .because("Repository abstractions belong to domain layer");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Repository implementations should be in infrastructure layer")
    void repositoryImplementationsShouldBeInInfrastructure() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("RepositoryImpl")
            .should().resideInAPackage("..infrastructure.persistence..")
            .because("Repository implementations are infrastructure concerns");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Use case interfaces should be in application port in")
    void useCaseInterfacesShouldBeInApplicationPortIn() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().areInterfaces()
            .should().resideInAPackage("..application.port.in..")
            .because("Use cases are input ports to the application");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Validate layered architecture dependencies")
    void validateLayeredArchitecture() {
        ArchRule rule = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            
            // Define layers
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            
            // Define access rules
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain")
            
            .because("Hexagonal architecture requires proper layer dependencies");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Domain events should be in domain layer")
    void domainEventsShouldBeInDomainLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Event")
            .should().resideInAPackage("..domain..")
            .because("Domain events are part of the domain model");
        
        rule.check(importedClasses);
    }
    
    @Test
    @DisplayName("Command and Query objects should be in application layer")
    void commandsAndQueriesShouldBeInApplicationLayer() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Command")
            .or().haveSimpleNameEndingWith("Query")
            .should().resideInAPackage("..application..")
            .because("Commands and queries are application concerns");
        
        rule.check(importedClasses);
    }
}