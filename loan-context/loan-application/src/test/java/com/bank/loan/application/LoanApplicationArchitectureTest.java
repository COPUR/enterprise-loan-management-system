package com.bank.loan.application;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LoanApplicationArchitectureTest {

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(LoanManagementService.class);

        noClasses()
            .that().resideInAPackage("com.bank.loan.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.bank.loan.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }
}
