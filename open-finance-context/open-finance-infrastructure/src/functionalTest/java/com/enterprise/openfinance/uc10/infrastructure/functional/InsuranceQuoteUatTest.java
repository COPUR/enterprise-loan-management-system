package com.enterprise.openfinance.uc10.infrastructure.functional;

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
        classes = InsuranceQuoteUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class InsuranceQuoteUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteQuoteToPolicyJourney() {
        Response create = request()
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                            "DriverDetails": {"Age":35,"LicenseDuration":10}
                          }
                        }
                        """)
                .when()
                .post("/open-insurance/v1/motor-insurance-quotes")
                .then()
                .statusCode(201)
                .header("X-OF-Idempotency", equalTo("MISS"))
                .body("Data.Quote.QuoteId", notNullValue())
                .extract()
                .response();

        String quoteId = create.path("Data.Quote.QuoteId");

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC10-1")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "Action": "ACCEPT",
                            "PaymentReference": "PAY-UAT-1",
                            "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                            "DriverDetails": {"Age":35,"LicenseDuration":10}
                          }
                        }
                        """)
                .when()
                .patch("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                .then()
                .statusCode(200)
                .header("X-OF-Idempotency", equalTo("MISS"))
                .body("Data.Quote.Status", equalTo("Accepted"))
                .body("Data.Quote.PolicyId", notNullValue())
                .body("Data.Quote.CertificateId", notNullValue());

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC10-1")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "Action": "ACCEPT",
                            "PaymentReference": "PAY-UAT-1",
                            "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                            "DriverDetails": {"Age":35,"LicenseDuration":10}
                          }
                        }
                        """)
                .when()
                .patch("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                .then()
                .statusCode(200)
                .header("X-OF-Idempotency", equalTo("HIT"));

        request()
                .when()
                .get("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                .then()
                .statusCode(200)
                .body("Data.Quote.Status", equalTo("Accepted"));
    }

    @Test
    void shouldRejectQuoteParameterManipulation() {
        Response create = request()
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "VehicleDetails": {"Make":"Toyota","Model":"Camry","Year":2023},
                            "DriverDetails": {"Age":35,"LicenseDuration":10}
                          }
                        }
                        """)
                .when()
                .post("/open-insurance/v1/motor-insurance-quotes")
                .then()
                .statusCode(201)
                .extract()
                .response();

        String quoteId = create.path("Data.Quote.QuoteId");

        request()
                .header("X-Idempotency-Key", "IDEMP-UAT-UC10-2")
                .contentType("application/json")
                .body("""
                        {
                          "Data": {
                            "Action": "ACCEPT",
                            "PaymentReference": "PAY-UAT-2",
                            "VehicleDetails": {"Make":"Nissan","Model":"Sunny","Year":2022},
                            "DriverDetails": {"Age":45,"LicenseDuration":20}
                          }
                        }
                        """)
                .when()
                .patch("/open-insurance/v1/motor-insurance-quotes/{quoteId}", quoteId)
                .then()
                .statusCode(400)
                .body("code", equalTo("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRejectUnsupportedAuthorizationHeader() {
        given().baseUri("http://localhost").port(port)
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Basic invalid")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc10-functional")
                .header("x-fapi-financial-id", "TPP-001")
                .when()
                .get("/open-insurance/v1/motor-insurance-quotes/Q-UNKNOWN")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"));
    }

    private RequestSpecification request() {
        return given().baseUri("http://localhost").port(port)
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc10-functional")
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
            "com.enterprise.openfinance.uc10.application",
            "com.enterprise.openfinance.uc10.infrastructure"
    })
    static class TestApplication {
    }
}
