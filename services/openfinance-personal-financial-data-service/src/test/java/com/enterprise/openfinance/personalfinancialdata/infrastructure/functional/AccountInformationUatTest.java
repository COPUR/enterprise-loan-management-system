package com.enterprise.openfinance.personalfinancialdata.infrastructure.functional;

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

@Tag("functional")
@Tag("e2e")
@SpringBootTest(
        classes = AccountInformationUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class AccountInformationUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteRetailAccountInformationJourney() {
        Response accountsResponse = request("CONS-AIS-001")
                .when()
                .get("/open-finance/v1/accounts")
                .then()
                .statusCode(200)
                .body("Data.Account.size()", greaterThan(0))
                .extract()
                .response();

        String accountId = accountsResponse.path("Data.Account[0].AccountId");

        request("CONS-AIS-001")
                .when()
                .get("/open-finance/v1/accounts/{accountId}", accountId)
                .then()
                .statusCode(200)
                .body("Data.Account.AccountId", equalTo(accountId));

        request("CONS-AIS-001")
                .when()
                .get("/open-finance/v1/accounts/{accountId}/balances", accountId)
                .then()
                .statusCode(200)
                .body("Data.Balance.size()", greaterThan(0));

        request("CONS-AIS-001")
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .queryParam("page", 1)
                .queryParam("pageSize", 50)
                .when()
                .get("/open-finance/v1/accounts/{accountId}/transactions", accountId)
                .then()
                .statusCode(200)
                .body("Meta.TotalPages", greaterThan(1));
    }

    @Test
    void shouldEnforceConsentScopesAndBolaProtection() {
        request("CONS-AIS-BAL-ONLY")
                .when()
                .get("/open-finance/v1/accounts/ACC-001/transactions")
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));

        request("CONS-AIS-001")
                .when()
                .get("/open-finance/v1/accounts/ACC-003")
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));
    }

    @Test
    void shouldSupportCorporateMultiCurrencyVisibility() {
        request("CONS-AIS-001")
                .when()
                .get("/open-finance/v1/accounts")
                .then()
                .statusCode(200)
                .body("Data.Account.findAll { it.Currency == 'USD' }.size()", greaterThan(0));
    }

    private RequestSpecification request(String consentId) {
        return given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-personalfinancialdata-functional")
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
            "com.enterprise.openfinance.personalfinancialdata.application",
            "com.enterprise.openfinance.personalfinancialdata.infrastructure"
    })
    static class TestApplication {
    }
}
