package com.bank.risk.application;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class RiskApplicationArchitectureTest {

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bank.risk.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bank.risk.infrastructure..")
                .allowEmptyShould(true);

        rule.check(new ClassFileImporter().importPackages("com.bank.risk"));
    }
}
