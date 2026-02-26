package com.bank.payment.infrastructure.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentContextOpenApiContractTest {

    @Test
    void shouldDefineImplementedPaymentEndpoints() throws IOException {
        String spec = loadSpec();

        assertThat(spec).doesNotContain("paths: {}");
        assertThat(spec).contains("\n  /api/v1/payments:\n");
        assertThat(spec).contains("\n  /api/v1/payments/{paymentId}:\n");
        assertThat(spec).contains("\n  /api/v1/payments/{paymentId}/confirm:\n");
        assertThat(spec).contains("\n  /api/v1/payments/{paymentId}/fail:\n");
        assertThat(spec).contains("\n  /api/v1/payments/{paymentId}/cancel:\n");
        assertThat(spec).contains("\n  /api/v1/payments/{paymentId}/refund:\n");
    }

    @Test
    void shouldRequireDpopForProtectedOperations() throws IOException {
        String spec = loadSpec();

        assertThat(spec).contains("name: DPoP");
        assertThat(spec).contains("required: true");
        assertThat(spec).contains("/api/v1/payments:");
        assertThat(spec).contains("security:");
    }

    private static String loadSpec() throws IOException {
        List<Path> candidates = List.of(
                Path.of("api/openapi/payment-context.yaml"),
                Path.of("../api/openapi/payment-context.yaml"),
                Path.of("../../api/openapi/payment-context.yaml"),
                Path.of("../../../api/openapi/payment-context.yaml")
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }

        throw new IOException("Unable to locate payment-context.yaml");
    }
}
