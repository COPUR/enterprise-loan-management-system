package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "openfinance.internal.secrets.storage=database",
                "openfinance.internal.security.jwt-hmac-secret=0123456789abcdef0123456789abcdef",
                "openfinance.internal.security.internal-username=test-service",
                "openfinance.internal.security.internal-password=test-service-password"
        }
)
@AutoConfigureMockMvc(addFilters = false)
class InternalSystemSecretApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldStoreSecretAsMaskedMetadataAndNeverExposeRawValue() throws Exception {
        String secretKey = "PAYMENT.RUNTIME.KEY." + UUID.randomUUID();
        String rawSecret = "ultra-sensitive-payment-secret";

        mockMvc.perform(post("/internal/v1/system/secrets")
                        .contentType("application/json")
                        .content("""
                                {
                                  "secretKey": "%s",
                                  "secretValue": "%s",
                                  "classification": "payment"
                                }
                                """.formatted(secretKey, rawSecret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretKey").value(secretKey.toUpperCase()))
                .andExpect(jsonPath("$.classification").value("PAYMENT"))
                .andExpect(jsonPath("$.maskedValue", not(containsString(rawSecret))))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get("/internal/v1/system/secrets/{secretKey}", secretKey.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretKey").value(secretKey.toUpperCase()))
                .andExpect(jsonPath("$.maskedValue", not(containsString(rawSecret))))
                .andExpect(jsonPath("$.version").value(1));
    }
}
