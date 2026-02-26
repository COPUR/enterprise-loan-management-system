package com.bank.loan.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanContextOpenApiContractTest {

    @Test
    void shouldDefineImplementedLoanEndpoints() throws IOException {
        String spec = loadSpec();

        assertThat(spec).doesNotContain("paths: {}");
        assertThat(spec).contains("\n  /api/v1/loans:\n");
        assertThat(spec).contains("\n  /api/v1/loans/{loanId}:\n");
        assertThat(spec).contains("\n  /api/v1/loans/{loanId}/approve:\n");
        assertThat(spec).contains("\n  /api/v1/loans/{loanId}/reject:\n");
        assertThat(spec).contains("\n  /api/v1/loans/{loanId}/disburse:\n");
        assertThat(spec).contains("\n  /api/v1/loans/{loanId}/payments:\n");
        assertThat(spec).contains("\n  /api/v1/loans/{loanId}/cancel:\n");
    }

    @Test
    void shouldRequireDpopForProtectedOperations() throws IOException {
        String spec = loadSpec();

        assertThat(spec).contains("name: DPoP");
        assertThat(spec).contains("required: true");
        assertThat(spec).contains("/api/v1/loans:");
        assertThat(spec).contains("security:");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/loan-context.yaml"),
                Path.of("../api/openapi/loan-context.yaml"),
                Path.of("../../api/openapi/loan-context.yaml"),
                Path.of("../../../api/openapi/loan-context.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate loan-context.yaml");
    }
}
