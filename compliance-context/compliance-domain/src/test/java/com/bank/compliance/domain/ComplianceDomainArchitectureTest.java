package com.bank.compliance.domain;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ComplianceDomainArchitectureTest {

    @Test
    void domainShouldNotDependOnApplicationOrInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bank.compliance.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bank.compliance.application..", "com.bank.compliance.infrastructure..")
                .allowEmptyShould(true);

        rule.check(new ClassFileImporter().importPackages("com.bank.compliance"));
    }
}
