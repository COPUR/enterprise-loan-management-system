package com.enterprise.openfinance.payeeverification.infrastructure.integration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = ConfirmationOfPayeeApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class ConfirmationOfPayeeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnMatchForExactName() throws Exception {
        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/confirmation-of-payee/confirmation"))
                        .contentType("application/json")
                        .content(payload("GB82WEST12345698765432", "IBAN", "Al Tareq Trading LLC")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-OF-Cache"))
                .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.Data.AccountStatus").value("Active"))
                .andExpect(jsonPath("$.Data.NameMatched").value("Match"))
                .andExpect(jsonPath("$.Data.MatchedName").value(Matchers.nullValue()));
    }

    @Test
    void shouldReturnCloseMatchAndMatchedName() throws Exception {
        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/confirmation-of-payee/confirmation"))
                        .contentType("application/json")
                        .content(payload("GB82WEST12345698765432", "IBAN", "Al Tariq Trading LLC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.AccountStatus").value("Active"))
                .andExpect(jsonPath("$.Data.NameMatched").value("CloseMatch"))
                .andExpect(jsonPath("$.Data.MatchedName").value("Al Tareq Trading LLC"));
    }

    @Test
    void shouldReturnNoMatchWithoutMatchedName() throws Exception {
        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/confirmation-of-payee/confirmation"))
                        .contentType("application/json")
                        .content(payload("GB82WEST12345698765432", "IBAN", "Random Corp")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.AccountStatus").value("Active"))
                .andExpect(jsonPath("$.Data.NameMatched").value("NoMatch"))
                .andExpect(jsonPath("$.Data.MatchedName").value(Matchers.nullValue()));
    }

    @Test
    void shouldReturnUnableToCheckForClosedAccount() throws Exception {
        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/confirmation-of-payee/confirmation"))
                        .contentType("application/json")
                        .content(payload("DE89370400440532013000", "IBAN", "Closed Legacy Account")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.AccountStatus").value("Closed"))
                .andExpect(jsonPath("$.Data.NameMatched").value("UnableToCheck"));
    }

    @Test
    void shouldRejectInvalidIban() throws Exception {
        mockMvc.perform(withSecurityHeaders(post("/open-finance/v1/confirmation-of-payee/confirmation"))
                        .contentType("application/json")
                        .content(payload("INVALID-IBAN", "IBAN", "Any Name")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("invalid IBAN")));
    }

    @Test
    void shouldReturnCacheHitOnSecondRequest() throws Exception {
        MockHttpServletRequestBuilder request = withSecurityHeaders(post("/open-finance/v1/confirmation-of-payee/confirmation"))
                .contentType("application/json")
                .content(payload("COP-CACHE-001", "BBAN", "Cache Test Beneficiary"));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"));
    }

    private static MockHttpServletRequestBuilder withSecurityHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-integration")
                .header("x-fapi-financial-id", "TPP-INTEGRATION")
                .accept("application/json");
    }

    private static String payload(String identification, String schemeName, String name) {
        return """
                {
                  "Data": {
                    "Identification": "%s",
                    "SchemeName": "%s",
                    "Name": "%s"
                  }
                }
                """.formatted(identification, schemeName, name);
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
            "com.enterprise.openfinance.payeeverification.application",
            "com.enterprise.openfinance.payeeverification.infrastructure"
    })
    static class TestApplication {
    }
}
