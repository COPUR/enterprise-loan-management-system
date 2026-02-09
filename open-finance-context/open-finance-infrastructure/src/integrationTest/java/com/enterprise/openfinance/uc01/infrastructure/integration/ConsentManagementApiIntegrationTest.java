package com.enterprise.openfinance.uc01.infrastructure.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = ConsentManagementApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class ConsentManagementApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAuthorizeListAndRevokeConsentViaApi() throws Exception {
        String customerId = "CUST-IT-1001";

        String createPayload = """
                {
                  "customerId": "%s",
                  "participantId": "TPP-IT-01",
                  "scopes": ["ReadAccounts", "ReadBalances"],
                  "purpose": "Integration test consent",
                  "expiresAt": "%s"
                }
                """.formatted(customerId, Instant.now().plus(2, ChronoUnit.DAYS));

        String createResponseBody = mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/consents"))
                        .contentType("application/json")
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.consentId").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createResponse = objectMapper.readTree(createResponseBody);
        String consentId = createResponse.path("consentId").asText();
        assertThat(consentId).startsWith("CONSENT-");

        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/consents/{consentId}/authorize", consentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(withSecurityHeaders(get("/open-finance/v1/consents/{consentId}", consentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.customerId").value(customerId));

        mockMvc.perform(withSecurityHeaders(get("/open-finance/v1/consents")
                        .param("customerId", customerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consentId").value(consentId));

        mockMvc.perform(withSecurityHeaders(patch("/open-finance/v1/consents/{consentId}/revoke", consentId))
                        .contentType("application/json")
                        .content("""
                                {"reason":"Customer requested revocation"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.revocationReason").value("Customer requested revocation"));
    }

    @Test
    void shouldRejectConsentWithInvalidPermissions() throws Exception {
        String createPayload = """
                {
                  "customerId": "CUST-IT-1002",
                  "participantId": "TPP-IT-02",
                  "scopes": ["ReadAdminData"],
                  "purpose": "Invalid scope test",
                  "expiresAt": "%s"
                }
                """.formatted(Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/consents"))
                        .contentType("application/json")
                        .content(createPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("invalid scope")));
    }

    @Test
    void shouldRejectAuthorizationHeaderThatIsNotDpop() throws Exception {
        String createPayload = """
                {
                  "customerId": "CUST-IT-1003",
                  "participantId": "TPP-IT-03",
                  "scopes": ["ReadAccounts"],
                  "purpose": "Header validation test",
                  "expiresAt": "%s"
                }
                """.formatted(Instant.now().plus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/open-finance/v1/consents")
                        .header("Authorization", "Bearer token")
                        .header("DPoP", "proof")
                        .header("X-FAPI-Interaction-ID", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(createPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("DPoP token type")));
    }

    private static MockHttpServletRequestBuilder withSecurityHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP uc01-integration-test-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", UUID.randomUUID().toString())
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
            "com.enterprise.openfinance.uc01.application",
            "com.enterprise.openfinance.uc01.infrastructure"
    })
    static class TestApplication {
    }
}
