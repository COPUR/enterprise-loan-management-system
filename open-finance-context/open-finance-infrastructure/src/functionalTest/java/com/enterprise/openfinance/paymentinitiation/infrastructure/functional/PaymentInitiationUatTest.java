package com.enterprise.openfinance.paymentinitiation.infrastructure.functional;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
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

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("functional")
@Tag("e2e")
@SpringBootTest(
        classes = PaymentInitiationUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class PaymentInitiationUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteImmediatePaymentJourney() {
        Response created = request("IDEMP-UAT-001")
                .body(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(201)
                .body("Data.Status", equalTo("AcceptedSettlementInProcess"))
                .body("Meta.IdempotencyReplay", equalTo(false))
                .extract()
                .response();

        String paymentId = created.path("Data.PaymentId");

        baseRequest()
                .when()
                .get("/open-finance/v1/payments/{paymentId}", paymentId)
                .then()
                .statusCode(200)
                .body("Data.PaymentId", equalTo(paymentId))
                .body("Data.Status", equalTo("AcceptedSettlementInProcess"));
    }

    @Test
    void shouldSupportIdempotentReplayAndConflict() {
        Response first = request("IDEMP-UAT-REPLAY")
                .body(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(201)
                .body("Meta.IdempotencyReplay", equalTo(false))
                .extract()
                .response();

        Response replay = request("IDEMP-UAT-REPLAY")
                .body(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", null))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(201)
                .body("Meta.IdempotencyReplay", equalTo(true))
                .extract()
                .response();

        org.junit.jupiter.api.Assertions.assertEquals(
                first.jsonPath().getString("Data.PaymentId"),
                replay.jsonPath().getString("Data.PaymentId")
        );

        request("IDEMP-UAT-REPLAY")
                .body(payload("101.00", "ACC-DEBTOR-001", "Vendor LLC", null))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT"));
    }

    @Test
    void shouldSupportFutureDatedAndRiskRejectedJourneys() {
        request("IDEMP-UAT-FUTURE")
                .body(payload("100.00", "ACC-DEBTOR-001", "Vendor LLC", LocalDate.now().plusDays(3).toString()))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(201)
                .body("Data.Status", equalTo("Pending"));

        request("IDEMP-UAT-RISK")
                .body(payload("100.00", "ACC-DEBTOR-001", "TEST_SANCTION_LIST", null))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(201)
                .body("Data.Status", equalTo("Rejected"));
    }

    @Test
    void shouldReturnBusinessErrorForInsufficientFunds() {
        request("IDEMP-UAT-LOW")
                .body(payload("100.00", "ACC-LOW-001", "Vendor LLC", null))
                .when()
                .post("/open-finance/v1/payments")
                .then()
                .statusCode(422)
                .body("code", equalTo("BUSINESS_RULE_VIOLATION"))
                .body("message", Matchers.containsString("Insufficient funds"));
    }

    private RequestSpecification request(String idempotencyKey) {
        return baseRequest()
                .header("X-Idempotency-Key", idempotencyKey)
                .header("x-jws-signature", "detached-signature");
    }

    private RequestSpecification baseRequest() {
        return given().baseUri("http://localhost").port(port)
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-payment-initiation-functional")
                .header("x-fapi-financial-id", "TPP-FUNCTIONAL");
    }

    private static String payload(String amount, String debtorAccountId, String creditorName, String requestedExecutionDate) {
        String requestedDateEntry = requestedExecutionDate == null
                ? ""
                : "\"RequestedExecutionDate\":\"" + requestedExecutionDate + "\",";
        return """
                {
                  "Data": {
                    "ConsentId": "CONS-001",
                    "Initiation": {
                      "InstructionIdentification": "INSTR-001",
                      "EndToEndIdentification": "E2E-001",
                      %s
                      "InstructedAmount": { "Amount": "%s", "Currency": "AED" },
                      "DebtorAccountId": "%s",
                      "CreditorName": "%s",
                      "CreditorAccount": {
                        "SchemeName": "IBAN",
                        "Identification": "AE120001000000123456789"
                      }
                    }
                  }
                }
                """.formatted(requestedDateEntry, amount, debtorAccountId, creditorName);
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
            "com.enterprise.openfinance.paymentinitiation.application",
            "com.enterprise.openfinance.paymentinitiation.infrastructure"
    })
    static class TestApplication {
    }
}
