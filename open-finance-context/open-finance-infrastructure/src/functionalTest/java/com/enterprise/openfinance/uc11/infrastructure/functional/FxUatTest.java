package com.enterprise.openfinance.uc11.infrastructure.functional;

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
        classes = FxUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class FxUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteQuoteToDealJourney() {
        Response quote = request()
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "SourceCurrency": "AED",
                            "TargetCurrency": "USD",
                            "SourceAmount": 1000.00
                          }
                        }
                        """)
                .when()
                .post("/open-finance/v1/fx-quotes")
                .then()
                .statusCode(200)
                .body("Data.Quote.QuoteId", notNullValue())
                .body("Data.Quote.ValidUntil", notNullValue())
                .extract()
                .response();

        String quoteId = quote.path("Data.Quote.QuoteId");

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC11-1")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "QuoteId": "%s"
                          }
                        }
                        """.formatted(quoteId))
                .when()
                .post("/open-finance/v1/fx-deals")
                .then()
                .statusCode(201)
                .header("X-OF-Idempotency", equalTo("MISS"))
                .body("Data.Deal.Status", equalTo("Booked"))
                .body("Data.Deal.DealId", notNullValue());

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC11-1")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "QuoteId": "%s"
                          }
                        }
                        """.formatted(quoteId))
                .when()
                .post("/open-finance/v1/fx-deals")
                .then()
                .statusCode(201)
                .header("X-OF-Idempotency", equalTo("HIT"));
    }

    @Test
    void shouldRejectExpiredQuote() {
        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC11-EXP")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "QuoteId": "Q-EXPIRED-001"
                          }
                        }
                        """)
                .when()
                .post("/open-finance/v1/fx-deals")
                .then()
                .statusCode(400)
                .body("code", equalTo("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRejectUnsupportedAuthorizationHeader() {
        given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Basic invalid")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc11-functional")
                .header("x-fapi-financial-id", "TPP-001")
                .body("{\"Data\":{\"SourceCurrency\":\"AED\",\"TargetCurrency\":\"USD\",\"SourceAmount\":1000.00}}")
                .when()
                .post("/open-finance/v1/fx-quotes")
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
                .header("X-FAPI-Interaction-ID", "ix-uc11-functional")
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
            "com.enterprise.openfinance.uc11.application",
            "com.enterprise.openfinance.uc11.infrastructure"
    })
    static class TestApplication {
    }
}
