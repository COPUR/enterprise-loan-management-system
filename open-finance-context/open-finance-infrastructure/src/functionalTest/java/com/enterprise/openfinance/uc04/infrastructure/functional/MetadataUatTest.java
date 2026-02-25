package com.enterprise.openfinance.uc04.infrastructure.functional;

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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("functional")
@Tag("e2e")
@SpringBootTest(
        classes = MetadataUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class MetadataUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteMetadataJourney() {
        Response transactions = request("CONS-META-001")
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .queryParam("page", 1)
                .queryParam("pageSize", 50)
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}/transactions", "ACC-001")
                .then()
                .statusCode(200)
                .body("Data.Transaction.size()", greaterThan(0))
                .body("Data.Transaction[0].MerchantDetails.Name", notNullValue())
                .extract()
                .response();

        String etag = transactions.getHeader("ETag");

        request("CONS-META-001")
                .header("If-None-Match", etag)
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .queryParam("page", 1)
                .queryParam("pageSize", 50)
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}/transactions", "ACC-001")
                .then()
                .statusCode(304);

        request("CONS-META-001")
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}/parties", "ACC-001")
                .then()
                .statusCode(200)
                .body("Data.Party.size()", greaterThan(0));

        request("CONS-META-001")
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}", "ACC-001")
                .then()
                .statusCode(200)
                .body("Data.Account.SchemeName", equalTo("IBAN"));

        request("CONS-META-001")
                .queryParam("accountId", "ACC-001")
                .when()
                .get("/open-finance/v1/metadata/standing-orders")
                .then()
                .statusCode(200)
                .body("Data.StandingOrder.size()", greaterThan(0))
                .body("Meta.TotalPages", greaterThanOrEqualTo(1));
    }

    @Test
    void shouldEnforceScopeAndBolaProtection() {
        request("CONS-META-TX-ONLY")
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}/parties", "ACC-001")
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));

        request("CONS-META-001")
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}/parties", "ACC-003")
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));
    }

    @Test
    void shouldSatisfyUatMetadataFields() {
        request("CONS-META-001")
                .when()
                .get("/open-finance/v1/metadata/accounts/{accountId}/transactions", "ACC-001")
                .then()
                .statusCode(200)
                .body("Data.Transaction[0].MerchantDetails.CategoryCode", notNullValue())
                .body("Data.Transaction[0].Amount.Currency", equalTo("AED"));
    }

    private RequestSpecification request(String consentId) {
        return given().baseUri("http://localhost").port(port)
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-uc04-functional")
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
            "com.enterprise.openfinance.uc04.application",
            "com.enterprise.openfinance.uc04.infrastructure"
    })
    static class TestApplication {
    }
}
