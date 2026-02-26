package com.enterprise.openfinance.insurancequotes.infrastructure.integration;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = InsuranceQuoteApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class InsuranceQuoteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAcceptReplayAndReadQuote() throws Exception {
        String createBody = """
                {
                  "Data": {
                    "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                    "DriverDetails": {"Age":35,"LicenseDuration":10}
                  }
                }
                """;

        MvcResult created = mockMvc.perform(withHeaders(post("/open-insurance/v1/motor-insurance-quotes")
                        .contentType("application/json")
                        .content(createBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-OF-Idempotency", "MISS"))
                .andExpect(jsonPath("$.Data.Quote.QuoteId").exists())
                .andReturn();

        String quoteId = JsonPathHelper.read(created.getResponse().getContentAsString(), "$.Data.Quote.QuoteId");

        String acceptBody = """
                {
                  "Data": {
                    "Action": "ACCEPT",
                    "PaymentReference": "PAY-1",
                    "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                    "DriverDetails": {"Age":35,"LicenseDuration":10}
                  }
                }
                """;

        mockMvc.perform(withHeaders(patch("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                        .header("X-Idempotency-Key", "IDEMP-INSURANCE-QUOTES-1")
                        .contentType("application/json")
                        .content(acceptBody)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Idempotency", "MISS"))
                .andExpect(jsonPath("$.Data.Quote.Status").value("Accepted"))
                .andExpect(jsonPath("$.Data.Quote.PolicyId").exists());

        mockMvc.perform(withHeaders(patch("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                        .header("X-Idempotency-Key", "IDEMP-INSURANCE-QUOTES-1")
                        .contentType("application/json")
                        .content(acceptBody)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Idempotency", "HIT"));

        MvcResult firstGet = mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = firstGet.getResponse().getHeader("ETag");

        mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                        .header("If-None-Match", etag)))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldRejectManipulationAndInvalidHeaders() throws Exception {
        String createBody = """
                {
                  "Data": {
                    "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                    "DriverDetails": {"Age":35,"LicenseDuration":10}
                  }
                }
                """;

        MvcResult created = mockMvc.perform(withHeaders(post("/open-insurance/v1/motor-insurance-quotes")
                        .contentType("application/json")
                        .content(createBody)))
                .andExpect(status().isCreated())
                .andReturn();

        String quoteId = JsonPathHelper.read(created.getResponse().getContentAsString(), "$.Data.Quote.QuoteId");

        String manipulatedBody = """
                {
                  "Data": {
                    "Action": "ACCEPT",
                    "PaymentReference": "PAY-2",
                    "VehicleDetails": {"Make":"Nissan","Model":"Sunny","Year":2022},
                    "DriverDetails": {"Age":45,"LicenseDuration":20}
                  }
                }
                """;

        mockMvc.perform(withHeaders(patch("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                        .header("X-Idempotency-Key", "IDEMP-INSURANCE-QUOTES-2")
                        .contentType("application/json")
                        .content(manipulatedBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("bound to original inputs")));

        mockMvc.perform(get("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                        .header("Authorization", "Basic invalid")
                        .header("DPoP", "proof-jwt")
                        .header("X-FAPI-Interaction-ID", "ix-insurance-quotes-integration")
                        .header("x-fapi-financial-id", "TPP-001")
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-insurance-quotes-integration")
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
            "com.enterprise.openfinance.insurancequotes.application",
            "com.enterprise.openfinance.insurancequotes.infrastructure"
    })
    static class TestApplication {
    }
}
