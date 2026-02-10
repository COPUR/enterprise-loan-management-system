package com.enterprise.openfinance.bankingmetadata.infrastructure.integration;

import com.enterprise.openfinance.bankingmetadata.infrastructure.security.SecurityTestTokenFactory;
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
        classes = MetadataApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
                "openfinance.bankingmetadata.security.jwt-secret=0123456789abcdef0123456789abcdef",
                "openfinance.bankingmetadata.persistence.mode=inmemory",
                "openfinance.bankingmetadata.cache.mode=inmemory"
        }
)
@AutoConfigureMockMvc
class MetadataApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetTransactionsAndReturnCacheHitOnSecondRequest() throws Exception {
        MockHttpServletRequestBuilder request = withHeaders(
                get("/open-finance/v1/metadata/accounts/ACC-001/transactions"),
                "CONS-META-001",
                "GET",
                "/open-finance/v1/metadata/accounts/ACC-001/transactions",
                "read_transactions read_parties read_metadata read_standing_orders"
        );

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(jsonPath("$.Data.Transaction").isArray())
                .andExpect(jsonPath("$.Data.Transaction[0].MerchantDetails.Name").exists());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"));
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() throws Exception {
        MvcResult first = mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/transactions")
                                .queryParam("page", "1")
                                .queryParam("pageSize", "20"),
                        "CONS-META-001",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-001/transactions",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isOk())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/transactions")
                                .queryParam("page", "1")
                                .queryParam("pageSize", "20")
                                .header("If-None-Match", etag),
                        "CONS-META-001",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-001/transactions",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldGetPartiesAccountAndStandingOrdersMetadata() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/parties"),
                        "CONS-META-001",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-001/parties",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Party").isArray())
                .andExpect(jsonPath("$.Data.Party[0].FullLegalName").exists());

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001"),
                        "CONS-META-001",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-001",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Account.SchemeName").value("IBAN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/standing-orders")
                                .queryParam("accountId", "ACC-001"),
                        "CONS-META-001",
                        "GET",
                        "/open-finance/v1/metadata/standing-orders",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.StandingOrder").isArray())
                .andExpect(jsonPath("$.Meta.TotalPages").value(Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldRejectWhenScopeMissingOrBolaOrExpired() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/parties"),
                        "CONS-META-TX-ONLY",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-001/parties",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-003/parties"),
                        "CONS-META-001",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-003/parties",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/transactions"),
                        "CONS-META-EXPIRED",
                        "GET",
                        "/open-finance/v1/metadata/accounts/ACC-001/transactions",
                        "read_transactions read_parties read_metadata read_standing_orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("expired")));
    }

    @Test
    void shouldRejectMissingDpopProof() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_transactions read_parties read_metadata read_standing_orders");
        mockMvc.perform(get("/open-finance/v1/metadata/accounts/ACC-001/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-FAPI-Interaction-ID", "ix-bankingmetadata-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .header("X-Consent-ID", "CONS-META-001")
                        .accept("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInsufficientTokenScopes() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_transactions");
        String dpopProof = SecurityTestTokenFactory.dpopProof(
                "GET",
                "http://localhost/open-finance/v1/metadata/accounts/ACC-001/parties",
                accessToken
        );
        mockMvc.perform(get("/open-finance/v1/metadata/accounts/ACC-001/parties")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("DPoP", dpopProof)
                        .header("X-FAPI-Interaction-ID", "ix-bankingmetadata-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .header("X-Consent-ID", "CONS-META-001")
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
                .header("X-FAPI-Interaction-ID", "ix-bankingmetadata-integration")
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
            "com.enterprise.openfinance.bankingmetadata.application",
            "com.enterprise.openfinance.bankingmetadata.infrastructure"
    })
    static class TestApplication {
    }
}
