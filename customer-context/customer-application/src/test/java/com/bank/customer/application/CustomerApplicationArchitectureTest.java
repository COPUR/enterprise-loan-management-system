package com.bank.customer.application;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CustomerApplicationArchitectureTest {

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(CustomerManagementService.class);

        noClasses()
            .that().resideInAPackage("com.bank.customer.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.bank.customer.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }
}
