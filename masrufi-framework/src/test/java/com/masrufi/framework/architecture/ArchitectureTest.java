package com.masrufi.framework.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.hexagonalArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architectural Guardrails Tests for MasruFi Framework
 * 
 * This test class enforces architectural constraints and ensures
 * the MasruFi Framework follows established design patterns:
 * 
 * - Hexagonal Architecture (Ports & Adapters)
 * - Domain-Driven Design (DDD)
 * - Clean Architecture principles
 * - Dependency rules
 * - Package structure constraints
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@DisplayName("🏛️ MasruFi Framework Architecture Guardrails")
class ArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
        .importPackages("com.masrufi.framework");

    @Test
    @DisplayName("Should follow hexagonal architecture pattern")
    void shouldFollowHexagonalArchitecture() {
        ArchRule hexagonalArchitectureRule = hexagonalArchitecture()
            .domainModels("..domain.model..")
            .domainServices("..domain.service..")
            .applicationServices("..application..")
            .adapters("..infrastructure..")
            .and().whereLayer("Domain Models").mayOnlyBeAccessedByLayers("Domain Services", "Application Services", "Adapters")
            .and().whereLayer("Domain Services").mayOnlyBeAccessedByLayers("Application Services", "Adapters")
            .and().whereLayer("Application Services").mayOnlyBeAccessedByLayers("Adapters");

        hexagonalArchitectureRule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on infrastructure")
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain layer should not depend on Spring Framework")
    void domainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Services should be annotated with @Service")
    void servicesShouldBeAnnotated() {
        ArchRule rule = classes()
            .that().resideInAPackage("..service..")
            .and().haveSimpleNameEndingWith("Service")
            .should().beAnnotatedWith("org.springframework.stereotype.Service");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain models should be immutable value objects or aggregates")
    void domainModelsShouldBeImmutable() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.model..")
            .and().areNotEnums()
            .and().areNotInterfaces()
            .should().haveOnlyFinalFields()
            .orShould().beAnnotatedWith("lombok.Data")
            .orShould().beAnnotatedWith("lombok.Value");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("No cyclic dependencies between packages")
    void shouldNotHaveCyclicDependencies() {
        ArchRule rule = slices()
            .matching("com.masrufi.framework.(*)..")
            .should().beFreeOfCycles();

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Repository interfaces should be in domain ports")
    void repositoriesShouldBeInDomainPorts() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should().resideInAPackage("..domain.port.out..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Use cases should be in domain ports")
    void useCasesShouldBeInDomainPorts() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .and().areInterfaces()
            .should().resideInAPackage("..domain.port.in..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Infrastructure adapters should implement domain ports")
    void infrastructureAdaptersShouldImplementDomainPorts() {
        ArchRule rule = classes()
            .that().resideInAPackage("..infrastructure.adapter..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().implementAnInterfaceThat()
            .resideInAPackage("..domain.port..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Commands should be immutable")
    void commandsShouldBeImmutable() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Command")
            .should().haveOnlyFinalFields()
            .orShould().beAnnotatedWith("lombok.Value")
            .orShould().beAnnotatedWith("lombok.Data");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Events should be immutable")
    void eventsShouldBeImmutable() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Event")
            .should().haveOnlyFinalFields()
            .orShould().beAnnotatedWith("lombok.Value");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Exceptions should end with Exception")
    void exceptionsShouldFollowNamingConvention() {
        ArchRule rule = classes()
            .that().areAssignableTo(Exception.class)
            .should().haveSimpleNameEndingWith("Exception");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Configuration classes should be in config package")
    void configurationClassesShouldBeInConfigPackage() {
        ArchRule rule = classes()
            .that().areAnnotatedWith("org.springframework.context.annotation.Configuration")
            .should().resideInAPackage("..config..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Islamic finance models should follow Sharia compliance")
    void islamicFinanceModelsShouldFollowShariaCompliance() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.model..")
            .and().haveSimpleNameContaining("Islamic")
            .should().haveFieldOfType("ShariaComplianceValidation")
            .orShould().haveMethodThat().hasName("isShariaCompliant");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Money class should be used for all monetary amounts")
    void shouldUseMoneyClassForMonetaryAmounts() {
        ArchRule rule = noFields()
            .that().haveRawType(java.math.BigDecimal.class)
            .and().haveName("amount")
            .should().beDeclaredInClassesThat()
            .resideOutsideOfPackage("..domain.model.Money");

        rule.check(importedClasses);
    }
}