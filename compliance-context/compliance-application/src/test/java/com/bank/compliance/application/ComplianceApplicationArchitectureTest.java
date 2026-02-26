package com.bank.compliance.application;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ComplianceApplicationArchitectureTest {

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bank.compliance.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bank.compliance.infrastructure..")
                .allowEmptyShould(true);

        rule.check(new ClassFileImporter().importPackages("com.bank.compliance"));
    }
}
