package com.bank.customer.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Tag("unit")
class CustomerDomainArchitectureTest {

    @Test
    void domainDoesNotDependOnApplicationOrInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(Customer.class);

        noClasses()
            .that().resideInAPackage("com.bank.customer.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.bank.customer.application..", "com.bank.customer.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }
}
