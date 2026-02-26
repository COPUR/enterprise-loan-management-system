package com.enterprise.openfinance.fxservices.infrastructure.integration;

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
        classes = FxApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class FxApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateExecuteReplayAndGetQuote() throws Exception {
        String quoteBody = """
                {
                  "Data": {
                    "SourceCurrency": "AED",
                    "TargetCurrency": "USD",
                    "SourceAmount": 1000.00
                  }
                }
                """;

        MvcResult quoteResult = mockMvc.perform(withHeaders(post("/open-finance/v1/fx-quotes")
                        .contentType("application/json")
                        .content(quoteBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.Quote.QuoteId").exists())
                .andExpect(jsonPath("$.Data.Quote.ValidUntil").exists())
                .andReturn();

        String quoteId = JsonPathHelper.read(quoteResult.getResponse().getContentAsString(), "$.Data.Quote.QuoteId");

        String dealBody = """
                {
                  "Data": {
                    "QuoteId": "%s"
                  }
                }
                """.formatted(quoteId);

        mockMvc.perform(withHeaders(post("/open-finance/v1/fx-deals")
                        .header("X-Idempotency-Key", "IDEMP-FX-SERVICES-1")
                        .contentType("application/json")
                        .content(dealBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "MISS"))
                .andExpect(jsonPath("$.Data.Deal.Status").value("Booked"));

        mockMvc.perform(withHeaders(post("/open-finance/v1/fx-deals")
                        .header("X-Idempotency-Key", "IDEMP-FX-SERVICES-1")
                        .contentType("application/json")
                        .content(dealBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "HIT"));

        MvcResult getResult = mockMvc.perform(withHeaders(get("/open-finance/v1/fx-quotes/{quoteId}", quoteId)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = getResult.getResponse().getHeader("ETag");
        mockMvc.perform(withHeaders(get("/open-finance/v1/fx-quotes/{quoteId}", quoteId)
                        .header("If-None-Match", etag)))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldRejectExpiredAndInvalidHeaders() throws Exception {
        String dealBody = """
                {
                  "Data": {
                    "QuoteId": "Q-EXPIRED-001"
                  }
                }
                """;

        mockMvc.perform(withHeaders(post("/open-finance/v1/fx-deals")
                        .header("X-Idempotency-Key", "IDEMP-FX-SERVICES-EXP")
                        .contentType("application/json")
                        .content(dealBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Quote Expired")));

        mockMvc.perform(post("/open-finance/v1/fx-quotes")
                        .header("Authorization", "Basic invalid")
                        .header("DPoP", "proof-jwt")
                        .header("X-FAPI-Interaction-ID", "ix-fx-services-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .contentType("application/json")
                        .content("{\"Data\":{\"SourceCurrency\":\"AED\",\"TargetCurrency\":\"USD\",\"SourceAmount\":1000.00}}")
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-fx-services-integration")
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
            "com.enterprise.openfinance.fxservices.application",
            "com.enterprise.openfinance.fxservices.infrastructure"
    })
    static class TestApplication {
    }
}
