package com.enterprise.openfinance.businessfinancialdata.infrastructure.functional;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.security.SecurityTestTokenFactory;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;

@Tag("functional")
@Tag("e2e")
@Tag("uat")
@SpringBootTest(
        classes = CorporateTreasuryUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
                "openfinance.businessfinancialdata.security.jwt-secret=0123456789abcdef0123456789abcdef",
                "openfinance.businessfinancialdata.persistence.mode=inmemory",
                "openfinance.businessfinancialdata.cache.mode=inmemory"
        }
)
class CorporateTreasuryUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteCorporateTreasuryJourney() {
        Response accounts = request("CONS-TRSY-001", "GET", "/open-finance/v1/corporate/accounts")
                .queryParam("includeVirtual", true)
                .queryParam("masterAccountId", "ACC-M-001")
                .when()
                .get("/open-finance/v1/corporate/accounts")
                .then()
                .statusCode(200)
                .body("Data.Account.size()", greaterThan(1))
                .extract()
                .response();

        String masterAccountId = accounts.path("Data.Account.find { it.Virtual == false }.AccountId");

        request("CONS-TRSY-001", "GET", "/open-finance/v1/corporate/accounts/" + masterAccountId + "/balances")
                .when()
                .get("/open-finance/v1/corporate/accounts/{masterAccountId}/balances", masterAccountId)
                .then()
                .statusCode(200)
                .body("Data.Balance.size()", greaterThan(0));

        request("CONS-TRSY-001", "GET", "/open-finance/v1/corporate/transactions")
                .queryParam("accountId", masterAccountId)
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .queryParam("page", 1)
                .queryParam("pageSize", 20)
                .when()
                .get("/open-finance/v1/corporate/transactions")
                .then()
                .statusCode(200)
                .body("Meta.TotalPages", greaterThanOrEqualTo(1));
    }

    @Test
    void shouldEnforceEntitlementAndDivisionAccess() {
        request("CONS-TRSY-RESTRICTED", "GET", "/open-finance/v1/corporate/accounts/ACC-M-001/balances")
                .when()
                .get("/open-finance/v1/corporate/accounts/ACC-M-001/balances")
                .then()
                .statusCode(200)
                .body("Data.Balance[0].Amount.Amount", equalTo("****"));

        request("CONS-TRSY-001", "GET", "/open-finance/v1/corporate/accounts/ACC-M-999/balances")
                .when()
                .get("/open-finance/v1/corporate/accounts/ACC-M-999/balances")
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"));
    }

    @Test
    void shouldExposeSweepingTransactionsForReconciliation() {
        request("CONS-TRSY-001", "GET", "/open-finance/v1/corporate/transactions")
                .queryParam("fromBookingDateTime", "2026-01-01T00:00:00Z")
                .queryParam("toBookingDateTime", "2026-12-31T00:00:00Z")
                .when()
                .get("/open-finance/v1/corporate/transactions")
                .then()
                .statusCode(200)
                .body("Data.Transaction.TransactionCode", hasItems("SWEEP"));
    }

    private RequestSpecification request(String consentId, String method, String path) {
        String accessToken = SecurityTestTokenFactory.accessToken("read_accounts read_balances read_transactions");
        String dpopProof = SecurityTestTokenFactory.dpopProof(method, "http://localhost:" + port + path, accessToken);
        return given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("DPoP", dpopProof)
                .header("X-FAPI-Interaction-ID", "ix-businessfinancialdata-functional")
                .header("x-fapi-financial-id", "TPP-001")
                .header("X-Consent-ID", consentId);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
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
            "com.enterprise.openfinance.businessfinancialdata.application",
            "com.enterprise.openfinance.businessfinancialdata.infrastructure"
    })
    static class TestApplication {
    }
}
