package com.enterprise.openfinance.corporatetreasury.infrastructure.integration;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = CorporateTreasuryApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class CorporateTreasuryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetAccountsAndReturnCacheHitOnSecondRequest() throws Exception {
        MockHttpServletRequestBuilder request = withHeaders(
                get("/open-finance/v1/corporate/accounts").queryParam("includeVirtual", "true"),
                "CONS-TRSY-001"
        );

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(jsonPath("$.Data.Account").isArray())
                .andExpect(jsonPath("$.Data.Account[0].AccountId").exists());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"));
    }

    @Test
    void shouldFilterByMasterAndIncludeVirtualAccounts() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/accounts")
                                .queryParam("includeVirtual", "true")
                                .queryParam("masterAccountId", "ACC-M-001"),
                        "CONS-TRSY-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Account[*].AccountId", Matchers.hasItems("ACC-M-001", "ACC-V-101")))
                .andExpect(jsonPath("$.Data.Account[?(@.Virtual==true)]").isNotEmpty());
    }

    @Test
    void shouldGetBalancesAndMaskForRestrictedEntitlement() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/accounts/ACC-M-001/balances"),
                        "CONS-TRSY-RESTRICTED"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Entitlement", "RESTRICTED"))
                .andExpect(jsonPath("$.Data.Balance[0].Amount.Amount").value("****"));
    }

    @Test
    void shouldReturnTransactionsAndSupportEtagNotModified() throws Exception {
        MvcResult first = mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/transactions")
                                .queryParam("accountId", "ACC-M-001")
                                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                                .queryParam("page", "1")
                                .queryParam("pageSize", "20"),
                        "CONS-TRSY-001"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.Data.Transaction[0].TransactionCode").exists())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/transactions")
                                .queryParam("accountId", "ACC-M-001")
                                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                                .queryParam("page", "1")
                                .queryParam("pageSize", "20")
                                .header("If-None-Match", etag),
                        "CONS-TRSY-001"))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldRejectScopeMissingBolaAndExpiredConsent() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/accounts/ACC-M-001/balances"),
                        "CONS-TRSY-ACCOUNTS"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/accounts/ACC-M-999/balances"),
                        "CONS-TRSY-001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/corporate/accounts").queryParam("includeVirtual", "true"),
                        "CONS-TRSY-EXPIRED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("expired")));
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder, String consentId) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-corporate-treasury-integration")
                .header("x-fapi-financial-id", "TPP-001")
                .header("X-Consent-ID", consentId)
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
            "com.enterprise.openfinance.corporatetreasury.application",
            "com.enterprise.openfinance.corporatetreasury.infrastructure"
    })
    static class TestApplication {
    }
}
