package com.enterprise.openfinance.personalfinancialdata.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalFinancialDataOpenApiContractTest {

    @Test
    void shouldContainImplementedAisEndpoints() throws IOException {
        String spec = loadSpec();

        assertThat(spec).contains("\n  /accounts:\n");
        assertThat(spec).contains("\n  /accounts/{AccountId}:\n");
        assertThat(spec).contains("\n  /accounts/{AccountId}/balances:\n");
        assertThat(spec).contains("\n  /accounts/{AccountId}/transactions:\n");
    }

    @Test
    void shouldRequireDpopHeaderForProtectedOperations() throws IOException {
        String spec = loadSpec();
        assertThat(spec).contains("DPoP:");
        assertThat(spec).contains("required: true");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/personal-financial-data-service.yaml"),
                Path.of("../api/openapi/personal-financial-data-service.yaml"),
                Path.of("../../api/openapi/personal-financial-data-service.yaml"),
                Path.of("../../../api/openapi/personal-financial-data-service.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate personal-financial-data-service.yaml");
    }
}
