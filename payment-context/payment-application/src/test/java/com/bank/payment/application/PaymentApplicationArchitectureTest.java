package com.bank.payment.application;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PaymentApplicationArchitectureTest {

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        JavaClasses classes = new ClassFileImporter().importPackagesOf(PaymentProcessingService.class);

        noClasses()
            .that().resideInAPackage("com.bank.payment.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.bank.payment.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }
}
