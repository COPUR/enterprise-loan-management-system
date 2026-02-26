package com.enterprise.openfinance.paymentinitiation.infrastructure.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = PaymentApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class PaymentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreatePaymentHappyPath() throws Exception {
        mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-001")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "MISS"))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.Data.ConsentId").value("CONS-001"))
                .andExpect(jsonPath("$.Data.Status").value("AcceptedSettlementInProcess"))
                .andExpect(jsonPath("$.Meta.IdempotencyReplay").value(false));
    }

    @Test
    void shouldReturnIdempotencyReplayForSameKeyAndPayload() throws Exception {
        MockHttpServletRequestBuilder request = withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-REPLAY")
                .contentType("application/json")
                .content(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null));

        MvcResult first = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "MISS"))
                .andReturn();

        MvcResult second = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "HIT"))
                .andExpect(jsonPath("$.Meta.IdempotencyReplay").value(true))
                .andReturn();

        String firstPaymentId = paymentId(first.getResponse().getContentAsString());
        String secondPaymentId = paymentId(second.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(secondPaymentId).isEqualTo(firstPaymentId);
    }

    @Test
    void shouldReturnConflictWhenIdempotencyKeyReusedWithDifferentPayload() throws Exception {
        mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-CONFLICT")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)))
                .andExpect(status().isCreated());

        mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-CONFLICT")
                        .contentType("application/json")
                        .content(payload("101.00", "ACC-DEBTOR-001", "Vendor LLC", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldReturnUnprocessableWhenFundsAreInsufficient() throws Exception {
        mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-INSUFFICIENT")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-LOW-001", "Vendor LLC", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void shouldCreatePendingForFutureDatedPayment() throws Exception {
        mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-FUTURE")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", LocalDate.now().plusDays(2).toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.Data.Status").value("Pending"));
    }

    @Test
    void shouldCreateRejectedForRiskSanctionsName() throws Exception {
        mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-RISK")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-DEBTOR-001", "TEST_SANCTION_LIST", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.Data.Status").value("Rejected"));
    }

    @Test
    void shouldGetPaymentStatusById() throws Exception {
        MvcResult createResult = mockMvc.perform(withPaymentHeaders(post("/open-finance/v1/payments"), "IDEMP-GET")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)))
                .andExpect(status().isCreated())
                .andReturn();

        String paymentId = paymentId(createResult.getResponse().getContentAsString());

        mockMvc.perform(withBaseHeaders(get("/open-finance/v1/payments/{paymentId}", paymentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.PaymentId").value(paymentId))
                .andExpect(jsonPath("$.Data.Status").value("AcceptedSettlementInProcess"));
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        mockMvc.perform(withBaseHeaders(post("/open-finance/v1/payments"))
                        .header("X-Idempotency-Key", "IDEMP-SIGN")
                        .header("x-jws-signature", "invalid-signature")
                        .contentType("application/json")
                        .content(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Signature Invalid"));
    }

    private MockHttpServletRequestBuilder withPaymentHeaders(MockHttpServletRequestBuilder builder, String idempotencyKey) {
        return withBaseHeaders(builder)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("x-jws-signature", "detached-signature");
    }

    private MockHttpServletRequestBuilder withBaseHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-payment-initiation-integration")
                .header("x-fapi-financial-id", "TPP-INTEGRATION")
                .accept("application/json");
    }

    private String paymentId(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        return root.path("Data").path("PaymentId").asText();
    }

    private static String payload(String amount, String debtorAccountId, String creditorName, String requestedExecutionDate) {
        String requestedDateEntry = requestedExecutionDate == null
                ? ""
                : "\"RequestedExecutionDate\":\"" + requestedExecutionDate + "\",";
        return """
                {
                  "Data": {
                    "ConsentId": "CONS-001",
                    "Initiation": {
                      "InstructionIdentification": "INSTR-001",
                      "EndToEndIdentification": "E2E-001",
                      %s
                      "InstructedAmount": { "Amount": "%s", "Currency": "AED" },
                      "DebtorAccountId": "%s",
                      "CreditorName": "%s",
                      "CreditorAccount": {
                        "SchemeName": "IBAN",
                        "Identification": "AE120001000000123456789"
                      }
                    }
                  }
                }
                """.formatted(requestedDateEntry, amount, debtorAccountId, creditorName);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            SecurityAutoConfiguration.class,
            OAuth2ResourceServerAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @ComponentScan(basePackages = {
            "com.enterprise.openfinance.paymentinitiation.application",
            "com.enterprise.openfinance.paymentinitiation.infrastructure"
    })
    static class TestApplication {
    }
}
