package com.bank.risk.domain;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class RiskDomainArchitectureTest {

    @Test
    void domainShouldNotDependOnApplicationOrInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.bank.risk.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.bank.risk.application..", "com.bank.risk.infrastructure..")
                .allowEmptyShould(true);

        rule.check(new ClassFileImporter().importPackages("com.bank.risk"));
    }
}
