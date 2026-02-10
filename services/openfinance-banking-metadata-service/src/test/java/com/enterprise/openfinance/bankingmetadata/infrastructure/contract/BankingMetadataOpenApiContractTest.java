package com.enterprise.openfinance.bankingmetadata.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BankingMetadataOpenApiContractTest {

    @Test
    void shouldContainImplementedMetadataEndpoints() throws IOException {
        String spec = loadSpec();

        assertThat(spec).contains("\n  /metadata/accounts/{AccountId}/transactions:\n");
        assertThat(spec).contains("\n  /metadata/accounts/{AccountId}/parties:\n");
        assertThat(spec).contains("\n  /metadata/accounts/{AccountId}:\n");
        assertThat(spec).contains("\n  /metadata/standing-orders:\n");
    }

    @Test
    void shouldRequireDpopHeaderForProtectedOperations() throws IOException {
        String spec = loadSpec();
        assertThat(spec).contains("DPoP:");
        assertThat(spec).contains("required: true");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/banking-metadata-service.yaml"),
                Path.of("../api/openapi/banking-metadata-service.yaml"),
                Path.of("../../api/openapi/banking-metadata-service.yaml"),
                Path.of("../../../api/openapi/banking-metadata-service.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate banking-metadata-service.yaml");
    }
}
