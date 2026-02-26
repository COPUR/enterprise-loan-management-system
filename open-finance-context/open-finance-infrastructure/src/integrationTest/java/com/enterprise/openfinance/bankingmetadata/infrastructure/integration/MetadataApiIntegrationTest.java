package com.enterprise.openfinance.bankingmetadata.infrastructure.integration;

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
        classes = MetadataApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class MetadataApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetTransactionsAndReturnCacheHitOnSecondRequest() throws Exception {
        MockHttpServletRequestBuilder request = withHeaders(
                get("/open-finance/v1/metadata/accounts/ACC-001/transactions"),
                "CONS-META-001"
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
                        "CONS-META-001"))
                .andExpect(status().isOk())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/transactions")
                                .queryParam("page", "1")
                                .queryParam("pageSize", "20")
                                .header("If-None-Match", etag),
                        "CONS-META-001"))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldGetPartiesAccountAndStandingOrdersMetadata() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/parties"),
                        "CONS-META-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Party").isArray())
                .andExpect(jsonPath("$.Data.Party[0].FullLegalName").exists());

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001"),
                        "CONS-META-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Account.SchemeName").value("IBAN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/standing-orders")
                                .queryParam("accountId", "ACC-001"),
                        "CONS-META-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.StandingOrder").isArray())
                .andExpect(jsonPath("$.Meta.TotalPages").value(Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldRejectWhenScopeMissingOrBolaOrExpired() throws Exception {
        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/parties"),
                        "CONS-META-TX-ONLY"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-003/parties"),
                        "CONS-META-001"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(withHeaders(
                        get("/open-finance/v1/metadata/accounts/ACC-001/transactions"),
                        "CONS-META-EXPIRED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("expired")));
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder, String consentId) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-banking-metadata-integration")
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
            "com.enterprise.openfinance.bankingmetadata.application",
            "com.enterprise.openfinance.bankingmetadata.infrastructure"
    })
    static class TestApplication {
    }
}
