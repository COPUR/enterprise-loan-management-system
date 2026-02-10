package com.enterprise.openfinance.businessfinancialdata.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessFinancialDataOpenApiContractTest {

    @Test
    void shouldMatchImplementedCorporateAisPaths() throws IOException {
        String spec = loadSpec();

        assertThat(spec).contains("\n  /accounts:\n");
        assertThat(spec).contains("\n  /accounts/{masterAccountId}/balances:\n");
        assertThat(spec).contains("\n  /transactions:\n");
        assertThat(spec).doesNotContain("\n  /scheduled-payments:\n");
        assertThat(spec).doesNotContain("\n  /parties:\n");
    }

    @Test
    void shouldRequireDpopForProtectedOperations() throws IOException {
        String spec = loadSpec();
        assertThat(spec).contains("DPoP:");
        assertThat(spec).contains("required: true");
    }

    @Test
    void shouldUseCorporateBaseServerPath() throws IOException {
        String spec = loadSpec();
        assertThat(spec).contains("https://api.example.com/open-finance/v1/corporate");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/business-financial-data-service.yaml"),
                Path.of("../api/openapi/business-financial-data-service.yaml"),
                Path.of("../../api/openapi/business-financial-data-service.yaml"),
                Path.of("../../../api/openapi/business-financial-data-service.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate business-financial-data-service.yaml");
    }
}
