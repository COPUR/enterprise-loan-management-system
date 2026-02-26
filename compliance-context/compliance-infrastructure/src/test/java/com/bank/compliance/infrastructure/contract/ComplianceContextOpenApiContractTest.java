package com.bank.compliance.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceContextOpenApiContractTest {

    @Test
    void shouldDefineImplementedComplianceEndpoints() throws IOException {
        String spec = loadSpec();

        assertThat(spec).doesNotContain("paths: {}");
        assertThat(spec).contains("\n  /api/v1/compliance/screen:\n");
        assertThat(spec).contains("\n  /api/v1/compliance/screenings/{transactionId}:\n");
    }

    @Test
    void shouldRequireDpopForProtectedOperations() throws IOException {
        String spec = loadSpec();
        assertThat(spec).contains("name: DPoP");
        assertThat(spec).contains("required: true");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/compliance-context.yaml"),
                Path.of("../api/openapi/compliance-context.yaml"),
                Path.of("../../api/openapi/compliance-context.yaml"),
                Path.of("../../../api/openapi/compliance-context.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate compliance-context.yaml");
    }
}
