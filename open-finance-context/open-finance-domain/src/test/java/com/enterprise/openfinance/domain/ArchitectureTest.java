package com.enterprise.openfinance.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests for Open Finance Domain following TDD approach.
 * These tests enforce hexagonal architecture and clean code principles.
 */
@Tag("architecture")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
                .importPackages("com.enterprise.openfinance");
    }

    @Test
    void domain_should_follow_hexagonal_architecture() {
        // Given: Hexagonal architecture layers
        ArchRule rule = onionArchitecture()
                .domainModels("..domain.model..")
                .domainServices("..domain.service..")
                .applicationServices("..application..")
                .adapter("persistence", "..infrastructure.adapter.output.persistence..")
                .adapter("web", "..infrastructure.adapter.input.rest..")
                .adapter("messaging", "..infrastructure.adapter.input.event..")
                .adapter("external", "..infrastructure.adapter.output.cbuae..");

        // When & Then: Architecture should be valid
        rule.check(classes);
    }

    @Test
    void domain_layer_should_not_depend_on_infrastructure() {
        // Given: Domain layer classes
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..");

        // When & Then: No infrastructure dependencies in domain
        rule.check(classes);
    }

    @Test
    void domain_layer_should_not_depend_on_spring_framework() {
        // Given: Domain layer classes
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..");

        // When & Then: No Spring dependencies in domain
        rule.check(classes);
    }

    @Test
    void domain_layer_should_not_depend_on_application_layer() {
        // Given: Domain layer classes
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..");

        // When & Then: Domain should be independent of application
        rule.check(classes);
    }

    @Test
    void application_layer_should_not_depend_on_infrastructure() {
        // Given: Application layer classes
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..");

        // When & Then: Application should not depend on infrastructure
        rule.check(classes);
    }

    @Test
    void ports_should_be_interfaces() {
        // Given: Port classes
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.port..")
                .should().beInterfaces()
                .andShould().bePublic();

        // When & Then: All ports should be public interfaces
        rule.check(classes);
    }

    @Test
    void repositories_should_be_interfaces_in_domain_ports() {
        // Given: Repository classes
        ArchRule rule = classes()
                .that().haveNameMatching(".*Repository")
                .should().beInterfaces()
                .andShould().resideInAPackage("..domain.port.output..");

        // When & Then: Repositories should be interfaces in domain ports
        rule.check(classes);
    }

    @Test
    void use_cases_should_be_interfaces_in_domain_ports() {
        // Given: Use case classes
        ArchRule rule = classes()
                .that().haveNameMatching(".*UseCase")
                .should().beInterfaces()
                .andShould().resideInAPackage("..domain.port.input..");

        // When & Then: Use cases should be interfaces in domain ports
        rule.check(classes);
    }

    @Test
    void domain_services_should_be_annotated() {
        // Given: Domain service classes
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.service..")
                .and().areNotInterfaces()
                .should().beAnnotatedWith("org.springframework.stereotype.Service");

        // When & Then: Domain services should be annotated (when Spring is used)
        rule.because("Domain services need to be managed by IoC container")
                .check(classes);
    }

    @Test
    void aggregates_should_be_in_model_package() {
        // Given: Aggregate classes
        ArchRule rule = classes()
                .that().areAnnotatedWith("com.enterprise.shared.domain.AggregateRoot")
                .should().resideInAPackage("..domain.model..");

        // When & Then: Aggregates should be in model package
        rule.check(classes);
    }

    @Test
    void value_objects_should_be_immutable() {
        // Given: Value object classes
        ArchRule rule = classes()
                .that().areAnnotatedWith("com.enterprise.shared.domain.ValueObject")
                .should().haveOnlyFinalFields()
                .andShould().haveOnlyPrivateConstructors().orShould().haveOnlyPackagePrivateConstructors();

        // When & Then: Value objects should be immutable
        rule.check(classes);
    }

    @Test
    void events_should_be_immutable() {
        // Given: Domain event classes
        ArchRule rule = classes()
                .that().haveNameMatching(".*Event")
                .and().resideInAPackage("..domain.event..")
                .should().haveOnlyFinalFields();

        // When & Then: Events should be immutable
        rule.check(classes);
    }

    @Test
    void commands_should_be_immutable() {
        // Given: Command classes
        ArchRule rule = classes()
                .that().haveNameMatching(".*Command")
                .should().haveOnlyFinalFields();

        // When & Then: Commands should be immutable
        rule.check(classes);
    }

    @Test
    void adapters_should_implement_ports() {
        // Given: Adapter classes in infrastructure
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure.adapter..")
                .and().haveNameMatching(".*Adapter")
                .should().implement(classes().that().resideInAPackage("..domain.port.."));

        // When & Then: Adapters should implement domain ports
        rule.check(classes);
    }

    @Test
    void no_cyclic_dependencies_between_packages() {
        // Given: Package structure
        ArchRule rule = slices()
                .matching("com.enterprise.openfinance.(*)..")
                .should().beFreeOfCycles();

        // When & Then: No cyclic dependencies
        rule.check(classes);
    }

    @Test
    void consent_aggregate_should_be_properly_designed() {
        // Given: Consent aggregate
        ArchRule rule = classes()
                .that().haveSimpleName("Consent")
                .should().beAnnotatedWith("com.enterprise.shared.domain.AggregateRoot")
                .andShould().resideInAPackage("..domain.model.consent..")
                .andShould().haveOnlyFinalFields().orShould().haveFields().that().arePrivate();

        // When & Then: Consent should follow aggregate design
        rule.check(classes);
    }

    @Test
    void participant_entity_should_be_properly_designed() {
        // Given: Participant entity
        ArchRule rule = classes()
                .that().haveSimpleName("Participant")
                .should().beAnnotatedWith("com.enterprise.shared.domain.Entity")
                .andShould().resideInAPackage("..domain.model.participant..");

        // When & Then: Participant should follow entity design
        rule.check(classes);
    }

    @Test
    void money_class_should_be_used_for_monetary_amounts() {
        // Given: Classes that might handle money
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().haveFieldsWithType(Double.class)
                .orShould().haveFieldsWithType(Float.class)
                .orShould().haveFieldsWithType(double.class)
                .orShould().haveFieldsWithType(float.class);

        // When & Then: No primitive money types
        rule.because("Money should be represented using Money value object")
                .check(classes);
    }

    @Test
    void infrastructure_adapters_should_not_be_public_except_controllers() {
        // Given: Infrastructure adapter classes
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure.adapter..")
                .and().doNotHaveSimpleNameEndingWith("Controller")
                .should().notBePublic();

        // When & Then: Adapters should have package visibility except controllers
        rule.because("Adapters should not expose implementation details")
                .check(classes);
    }

    @Test
    void cbuae_integration_should_be_isolated() {
        // Given: CBUAE integration classes
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure.adapter.output.cbuae..")
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(
                        "..infrastructure.adapter.output.cbuae..",
                        "..infrastructure.config..",
                        "..application.."
                );

        // When & Then: CBUAE integration should be encapsulated
        rule.check(classes);
    }
}