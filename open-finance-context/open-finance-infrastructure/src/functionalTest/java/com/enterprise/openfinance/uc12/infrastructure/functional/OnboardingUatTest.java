package com.enterprise.openfinance.uc12.infrastructure.functional;

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
import static org.hamcrest.Matchers.notNullValue;

@Tag("functional")
@Tag("e2e")
@Tag("uat")
@SpringBootTest(
        classes = OnboardingUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class OnboardingUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteOnboardingJourneyWithReplay() {
        Response created = request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC12-1")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "EncryptedKycPayload": "jwe:Alice Ahmed|7841987001|AE",
                            "PreferredCurrency": "USD"
                          }
                        }
                        """)
                .when()
                .post("/open-finance/v1/accounts")
                .then()
                .statusCode(201)
                .header("X-OF-Idempotency", equalTo("MISS"))
                .body("Data.Account.AccountId", notNullValue())
                .extract()
                .response();

        String accountId = created.path("Data.Account.AccountId");

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC12-1")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "EncryptedKycPayload": "jwe:Alice Ahmed|7841987001|AE",
                            "PreferredCurrency": "USD"
                          }
                        }
                        """)
                .when()
                .post("/open-finance/v1/accounts")
                .then()
                .statusCode(201)
                .header("X-OF-Idempotency", equalTo("HIT"));

        request()
                .when()
                .get("/open-finance/v1/accounts/{accountId}", accountId)
                .then()
                .statusCode(200)
                .body("Data.Account.Status", equalTo("Opened"));
    }

    @Test
    void shouldRejectDecryptionFailureAndSanctionsHit() {
        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC12-BAD")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "EncryptedKycPayload": "bad-payload",
                            "PreferredCurrency": "USD"
                          }
                        }
                        """)
                .when()
                .post("/open-finance/v1/accounts")
                .then()
                .statusCode(400)
                .body("code", equalTo("DECRYPTION_FAILED"));

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC12-SANCTION")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "EncryptedKycPayload": "jwe:TEST_BLOCKED|7841987009|AE",
                            "PreferredCurrency": "USD"
                          }
                        }
                        """)
                .when()
                .post("/open-finance/v1/accounts")
                .then()
                .statusCode(403)
                .body("code", equalTo("COMPLIANCE_VIOLATION"));
    }

    @Test
    void shouldRejectUnsupportedAuthorizationHeader() {
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Basic invalid")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc12-functional")
                .header("x-fapi-financial-id", "TPP-001")
                .header("X-Idempotency-Key", "IDEMP-UAT-UC12-INVALID")
                .body("{\"Data\":{\"EncryptedKycPayload\":\"jwe:Alice Ahmed|7841987001|AE\",\"PreferredCurrency\":\"USD\"}}")
                .when()
                .post("/open-finance/v1/accounts")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"));
    }

    private RequestSpecification request() {
        return given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc12-functional")
                .header("x-fapi-financial-id", "TPP-001");
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
            "com.enterprise.openfinance.uc12.application",
            "com.enterprise.openfinance.uc12.infrastructure"
    })
    static class TestApplication {
    }
}
