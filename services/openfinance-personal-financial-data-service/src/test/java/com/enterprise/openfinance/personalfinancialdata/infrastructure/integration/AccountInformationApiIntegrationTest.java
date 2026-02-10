package com.enterprise.openfinance.personalfinancialdata.infrastructure.integration;

import com.enterprise.openfinance.personalfinancialdata.infrastructure.security.SecurityTestTokenFactory;
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
        classes = AccountInformationApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
                "openfinance.personalfinancialdata.security.jwt-secret=0123456789abcdef0123456789abcdef",
                "openfinance.personalfinancialdata.persistence.mode=inmemory",
                "openfinance.personalfinancialdata.cache.mode=inmemory"
        }
)
@AutoConfigureMockMvc
class AccountInformationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetAccountsAndReturnCacheHitOnSecondRequest() throws Exception {
        MockHttpServletRequestBuilder request = withHeaders(
                get("/open-finance/v1/accounts"),
                "CONS-AIS-001",
                "GET",
                "/open-finance/v1/accounts",
                "read_accounts read_balances read_transactions"
        );

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(jsonPath("$.Data.Account").isArray())
                .andExpect(jsonPath("$.Data.Account[0].AccountId").exists())
                .andExpect(jsonPath("$.Data.Account[0].Currency").exists());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"));
    }

    @Test
    void shouldGetSpecificAccount() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/accounts/ACC-001"),
                        "CONS-AIS-001",
                        "GET",
                        "/open-finance/v1/accounts/ACC-001",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Account.AccountId").value("ACC-001"))
                .andExpect(jsonPath("$.Data.Account.IBAN").value(Matchers.containsString("*")));
    }

    @Test
    void shouldRejectBolaAccess() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/accounts/ACC-003"),
                        "CONS-AIS-001",
                        "GET",
                        "/open-finance/v1/accounts/ACC-003",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldGetBalances() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/accounts/ACC-001/balances"),
                        "CONS-AIS-001",
                        "GET",
                        "/open-finance/v1/accounts/ACC-001/balances",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(jsonPath("$.Data.Balance").isArray())
                .andExpect(jsonPath("$.Data.Balance[0].Amount.Currency").value("AED"));
    }

    @Test
    void shouldRejectTransactionsWhenScopeMissing() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/accounts/ACC-001/transactions"),
                        "CONS-AIS-BAL-ONLY",
                        "GET",
                        "/open-finance/v1/accounts/ACC-001/transactions",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldFilterAndPaginateTransactions() throws Exception {
        mockMvc.perform(withHeaders(get("/open-finance/v1/accounts/ACC-001/transactions")
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .queryParam("page", "1")
                .queryParam("pageSize", "50"),
                        "CONS-AIS-001",
                        "GET",
                        "/open-finance/v1/accounts/ACC-001/transactions",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.Data.Transaction").isArray())
                .andExpect(jsonPath("$.Meta.TotalPages").value(Matchers.greaterThan(1)))
                .andExpect(jsonPath("$.Links.Next").isNotEmpty());
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() throws Exception {
        MvcResult first = mockMvc.perform(withHeaders(get("/open-finance/v1/accounts/ACC-001/transactions")
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .queryParam("page", "1")
                .queryParam("pageSize", "20"),
                        "CONS-AIS-001",
                        "GET",
                        "/open-finance/v1/accounts/ACC-001/transactions",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isOk())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(withHeaders(get("/open-finance/v1/accounts/ACC-001/transactions")
                        .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                        .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                        .queryParam("page", "1")
                        .queryParam("pageSize", "20")
                        .header("If-None-Match", etag),
                        "CONS-AIS-001",
                        "GET",
                        "/open-finance/v1/accounts/ACC-001/transactions",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldRejectExpiredConsent() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/accounts"),
                        "CONS-AIS-EXPIRED",
                        "GET",
                        "/open-finance/v1/accounts",
                        "read_accounts read_balances read_transactions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("expired")));
    }

    @Test
    void shouldRejectMissingDpopProof() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_accounts read_balances read_transactions");
        mockMvc.perform(get("/open-finance/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-FAPI-Interaction-ID", "ix-personalfinancialdata-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .header("X-Consent-ID", "CONS-AIS-001")
                        .accept("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInsufficientTokenScopes() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_accounts");
        String dpopProof = SecurityTestTokenFactory.dpopProof(
                "GET",
                "http://localhost/open-finance/v1/accounts/ACC-001/transactions",
                accessToken
        );
        mockMvc.perform(get("/open-finance/v1/accounts/ACC-001/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("DPoP", dpopProof)
                        .header("X-FAPI-Interaction-ID", "ix-personalfinancialdata-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .header("X-Consent-ID", "CONS-AIS-001")
                        .accept("application/json"))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder,
                                                             String consentId,
                                                             String method,
                                                             String path,
                                                             String scopes) {
        String accessToken = SecurityTestTokenFactory.accessToken(scopes);
        String dpopProof = SecurityTestTokenFactory.dpopProof(method, "http://localhost" + path, accessToken);
        return builder
                .header("Authorization", "Bearer " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", "ix-personalfinancialdata-integration")
                .header("x-fapi-financial-id", "TPP-001")
                .header("X-Consent-ID", consentId)
                .accept("application/json");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
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
            "com.enterprise.openfinance.personalfinancialdata.application",
            "com.enterprise.openfinance.personalfinancialdata.infrastructure"
    })
    static class TestApplication {
    }
}
