package com.enterprise.openfinance.uc09.infrastructure.integration;

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
        classes = InsuranceDataApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class InsuranceDataApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListPoliciesSupportCacheAndGetPolicyDetails() throws Exception {
        MvcResult firstList = mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies")
                        .header("X-Consent-ID", "CONS-INS-001")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.Data.Policies[0].PolicyId").exists())
                .andExpect(jsonPath("$.Data.Policies[0].Vehicle.Year").isNumber())
                .andExpect(jsonPath("$.Data.Policies[0].Premium.Amount").isString())
                .andReturn();

        String policyId = JsonPathHelper.read(firstList.getResponse().getContentAsString(), "$.Data.Policies.0.PolicyId");

        MvcResult secondList = mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies")
                        .header("X-Consent-ID", "CONS-INS-001")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"))
                .andReturn();

        String listEtag = secondList.getResponse().getHeader("ETag");
        mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies")
                        .header("X-Consent-ID", "CONS-INS-001")
                        .header("If-None-Match", listEtag)))
                .andExpect(status().isNotModified());

        MvcResult firstPolicy = mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies/{policyId}", policyId)
                        .header("X-Consent-ID", "CONS-INS-001")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(jsonPath("$.Data.Policy.PolicyId").value(policyId))
                .andExpect(header().exists("ETag"))
                .andReturn();

        MvcResult secondPolicy = mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies/{policyId}", policyId)
                        .header("X-Consent-ID", "CONS-INS-001")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"))
                .andReturn();

        String policyEtag = secondPolicy.getResponse().getHeader("ETag");
        mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies/{policyId}", policyId)
                        .header("X-Consent-ID", "CONS-INS-001")
                        .header("If-None-Match", policyEtag)))
                .andExpect(status().isNotModified());

        assert firstPolicy != null;
    }

    @Test
    void shouldRejectInvalidSecurityHeadersAndConsentViolations() throws Exception {
        mockMvc.perform(get("/open-insurance/v1/motor-insurance-policies")
                        .header("Authorization", "Basic invalid")
                        .header("DPoP", "proof-jwt")
                        .header("X-FAPI-Interaction-ID", "ix-uc09-integration")
                        .header("X-Consent-ID", "CONS-INS-001")
                        .header("x-fapi-financial-id", "TPP-001")
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies")
                        .header("X-Consent-ID", "CONS-INS-RO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Required scope missing")));

        mockMvc.perform(withHeaders(get("/open-insurance/v1/motor-insurance-policies/{policyId}", "POL-MTR-003")
                        .header("X-Consent-ID", "CONS-INS-001")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("linked to consent")));
    }

    private static MockHttpServletRequestBuilder withHeaders(MockHttpServletRequestBuilder builder) {
        return builder
                .header("Authorization", "DPoP integration-token")
                .header("DPoP", "proof-jwt")
                .header("X-FAPI-Interaction-ID", "ix-uc09-integration")
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
            "com.enterprise.openfinance.uc09.application",
            "com.enterprise.openfinance.uc09.infrastructure"
    })
    static class TestApplication {
    }
}
