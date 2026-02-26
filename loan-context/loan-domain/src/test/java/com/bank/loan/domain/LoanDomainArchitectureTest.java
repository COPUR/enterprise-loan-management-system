package com.bank.loan.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LoanDomainArchitectureTest {

    @Test
    void domainDoesNotDependOnApplicationOrInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(Loan.class);

        noClasses()
            .that().resideInAPackage("com.bank.loan.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.bank.loan.application..", "com.bank.loan.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }
}
