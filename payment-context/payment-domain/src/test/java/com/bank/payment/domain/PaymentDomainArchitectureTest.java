package com.bank.payment.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PaymentDomainArchitectureTest {

    @Test
    void domainDoesNotDependOnApplicationOrInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(Payment.class);

        noClasses()
            .that().resideInAPackage("com.bank.payment.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.bank.payment.application..", "com.bank.payment.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }
}
