package com.enterprise.openfinance.uc12.infrastructure.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = OnboardingApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class OnboardingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateReplayAndGetOnboardingAccount() throws Exception {
        String createBody = """
                {
                  "Data": {
                    "EncryptedKycPayload": "jwe:Alice Ahmed|7841987001|AE",
                    "PreferredCurrency": "USD"
                  }
                }
                """;

        MvcResult createResult = mockMvc.perform(withHeaders(post("/open-finance/v1/accounts")
                        .header("X-Idempotency-Key", "IDEMP-UC12-1")
                        .contentType("application/json")
                        .content(createBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "MISS"))
                .andExpect(jsonPath("$.Data.Account.AccountId").exists())
                .andReturn();

        String accountId = JsonPathHelper.read(createResult.getResponse().getContentAsString(), "$.Data.Account.AccountId");

        mockMvc.perform(withHeaders(post("/open-finance/v1/accounts")
                        .header("X-Idempotency-Key", "IDEMP-UC12-1")
                        .contentType("application/json")
                        .content(createBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "HIT"));

        MvcResult getResult = mockMvc.perform(withHeaders(get("/open-finance/v1/accounts/{accountId}", accountId)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = getResult.getResponse().getHeader("ETag");
        mockMvc.perform(withHeaders(get("/open-finance/v1/accounts/{accountId}", accountId)
                        .header("If-None-Match", etag)))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldRejectDecryptionSanctionsAndInvalidAuthorization() throws Exception {
        String badDecryptBody = """
                {
                  "Data": {
                    "EncryptedKycPayload": "bad-payload",
                    "PreferredCurrency": "USD"
                  }
                }
                """;

        mockMvc.perform(withHeaders(post("/open-finance/v1/accounts")
                        .header("X-Idempotency-Key", "IDEMP-UC12-BAD")
                        .contentType("application/json")
                        .content(badDecryptBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DECRYPTION_FAILED"));

        String sanctionsBody = """
                {
                  "Data": {
                    "EncryptedKycPayload": "jwe:TEST_BLOCKED|7841987009|AE",
                    "PreferredCurrency": "USD"
                  }
                }
                """;

        mockMvc.perform(withHeaders(post("/open-finance/v1/accounts")
                        .header("X-Idempotency-Key", "IDEMP-UC12-SANCTION")
                        .contentType("application/json")
                        .content(sanctionsBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPLIANCE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Onboarding Rejected")));

        mockMvc.perform(post("/open-finance/v1/accounts")
                        .header("Authorization", "Basic invalid")
                        .header("DPoP", "proof-jwt")
                        .header("X-FAPI-Interaction-ID", "ix-uc12-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .header("X-Idempotency-Key", "IDEMP-UC12-INVALID")
                        .contentType("application/json")
                        .content("{\"Data\":{\"EncryptedKycPayload\":\"jwe:Alice Ahmed|7841987001|AE\",\"PreferredCurrency\":\"USD\"}}")
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-uc12-integration")
                .header("x-fapi-financial-id", "TPP-001")
                .accept("application/json");
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
            "com.enterprise.openfinance.uc12.application",
            "com.enterprise.openfinance.uc12.infrastructure"
    })
    static class TestApplication {
    }
}
