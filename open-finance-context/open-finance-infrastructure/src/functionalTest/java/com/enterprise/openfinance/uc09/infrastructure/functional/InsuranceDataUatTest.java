package com.enterprise.openfinance.uc09.infrastructure.functional;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("functional")
@Tag("e2e")
@Tag("uat")
@SpringBootTest(
        classes = InsuranceDataUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class InsuranceDataUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteInsuranceDataJourneyWithCacheAndPolicyDetail() {
        Response firstList = request("CONS-INS-001")
                .when()
                .get("/open-insurance/v1/motor-insurance-policies")
                .then()
                .statusCode(200)
                .header("X-OF-Cache", equalTo("MISS"))
                .body("Data.Policies.size()", greaterThanOrEqualTo(1))
                .body("Data.Policies[0].PolicyId", notNullValue())
                .extract()
                .response();

        String policyId = firstList.path("Data.Policies[0].PolicyId");

        request("CONS-INS-001")
                .when()
                .get("/open-insurance/v1/motor-insurance-policies")
                .then()
                .statusCode(200)
                .header("X-OF-Cache", equalTo("HIT"));

        request("CONS-INS-001")
                .when()
                .get("/open-insurance/v1/motor-insurance-policies/{policyId}", policyId)
                .then()
                .statusCode(200)
                .body("Data.Policy.PolicyId", equalTo(policyId))
                .body("Data.Policy.Status", equalTo("Active"));
    }

    @Test
    void shouldRejectInsufficientConsentScope() {
        request("CONS-INS-RO")
                .when()
                .get("/open-insurance/v1/motor-insurance-policies")
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));
    }

    @Test
    void shouldRejectUnsupportedAuthorizationHeader() {
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Basic invalid")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc09-functional")
                .header("x-fapi-financial-id", "TPP-001")
                .header("X-Consent-ID", "CONS-INS-001")
                .when()
                .get("/open-insurance/v1/motor-insurance-policies")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"));
    }

    private RequestSpecification request(String consentId) {
        return given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc09-functional")
                .header("x-fapi-financial-id", "TPP-001")
                .header("X-Consent-ID", consentId);
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
