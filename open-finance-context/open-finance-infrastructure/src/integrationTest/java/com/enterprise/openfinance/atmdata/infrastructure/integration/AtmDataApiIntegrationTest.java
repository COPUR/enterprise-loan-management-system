package com.enterprise.openfinance.atmdata.infrastructure.integration;

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

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest(
        classes = AtmDataApiIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
@AutoConfigureMockMvc(addFilters = false)
class AtmDataApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListAtmsAndSupportEtagRevalidation() throws Exception {
        MvcResult first = mockMvc.perform(get("/open-finance/v1/atms")
                        .header("X-FAPI-Interaction-ID", "ix-atm-data-int-1")
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "MISS"))
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.Data.ATM").isArray())
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(get("/open-finance/v1/atms")
                        .header("X-FAPI-Interaction-ID", "ix-atm-data-int-1")
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-OF-Cache", "HIT"));

        mockMvc.perform(get("/open-finance/v1/atms")
                        .header("X-FAPI-Interaction-ID", "ix-atm-data-int-1")
                        .header("If-None-Match", etag)
                        .accept("application/json"))
                .andExpect(status().isNotModified());
    }

    @Test
    void shouldFilterByLocationAndRejectInvalidRequests() throws Exception {
        mockMvc.perform(get("/open-finance/v1/atms")
                        .header("X-FAPI-Interaction-ID", "ix-atm-data-int-2")
                        .param("lat", "25.2048")
                        .param("long", "55.2708")
                        .param("radius", "2")
                        .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.ATM.length()").value(1))
                .andExpect(jsonPath("$.Data.ATM[0].AtmId").value("ATM-001"));

        mockMvc.perform(get("/open-finance/v1/atms")
                        .header("X-FAPI-Interaction-ID", "ix-atm-data-int-3")
                        .header("Authorization", "Basic invalid")
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/open-finance/v1/atms")
                        .header("X-FAPI-Interaction-ID", "ix-atm-data-int-4")
                        .param("lat", "95")
                        .param("long", "55")
                        .accept("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
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
            "com.enterprise.openfinance.atmdata.application",
            "com.enterprise.openfinance.atmdata.infrastructure"
    })
    static class TestApplication {
    }
}
