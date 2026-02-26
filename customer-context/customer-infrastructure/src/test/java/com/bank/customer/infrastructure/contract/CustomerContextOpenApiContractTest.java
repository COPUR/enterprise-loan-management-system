package com.bank.customer.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerContextOpenApiContractTest {

    @Test
    void shouldDefineImplementedCustomerEndpoints() throws IOException {
        String spec = loadSpec();

        assertThat(spec).doesNotContain("paths: {}");
        assertThat(spec).contains("\n  /api/v1/customers:\n");
        assertThat(spec).contains("\n  /api/v1/customers/{customerId}:\n");
        assertThat(spec).contains("\n  /api/v1/customers/{customerId}/credit-limit:\n");
        assertThat(spec).contains("\n  /api/v1/customers/{customerId}/credit/reserve:\n");
        assertThat(spec).contains("\n  /api/v1/customers/{customerId}/credit/release:\n");
    }

    @Test
    void shouldRequireDpopForProtectedOperations() throws IOException {
        String spec = loadSpec();

        assertThat(spec).contains("name: DPoP");
        assertThat(spec).contains("required: true");
        assertThat(spec).contains("/api/v1/customers:");
        assertThat(spec).contains("security:");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/customer-context.yaml"),
                Path.of("../api/openapi/customer-context.yaml"),
                Path.of("../../api/openapi/customer-context.yaml"),
                Path.of("../../../api/openapi/customer-context.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate customer-context.yaml");
    }
}
