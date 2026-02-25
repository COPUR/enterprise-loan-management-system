package com.enterprise.openfinance.uc03.infrastructure.functional;

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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Tag("functional")
@Tag("e2e")
@SpringBootTest(
        classes = ConfirmationOfPayeeUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class ConfirmationOfPayeeUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldSupportExactCloseAndNoMatchJourney() {
        baseRequest()
                .body(payload("GB82WEST12345698765432", "IBAN", "Al Tareq Trading LLC"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(200)
                .body("Data.AccountStatus", equalTo("Active"))
                .body("Data.NameMatched", equalTo("Match"))
                .body("Data.MatchedName", nullValue());

        baseRequest()
                .body(payload("GB82WEST12345698765432", "IBAN", "Al Tariq Trading LLC"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(200)
                .body("Data.NameMatched", equalTo("CloseMatch"))
                .body("Data.MatchedName", equalTo("Al Tareq Trading LLC"));

        baseRequest()
                .body(payload("GB82WEST12345698765432", "IBAN", "Random Corp"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(200)
                .body("Data.NameMatched", equalTo("NoMatch"))
                .body("Data.MatchedName", nullValue());
    }

    @Test
    void shouldReturnUnableToCheckForClosedAccount() {
        baseRequest()
                .body(payload("DE89370400440532013000", "IBAN", "Closed Legacy Account"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(200)
                .body("Data.AccountStatus", equalTo("Closed"))
                .body("Data.NameMatched", equalTo("UnableToCheck"));
    }

    @Test
    void shouldRejectInvalidIbanAndUseInternalCacheOnRetry() {
        baseRequest()
                .body(payload("INVALID-IBAN", "IBAN", "Any Name"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"))
                .body("message", Matchers.containsString("invalid IBAN"));

        Response first = baseRequest()
                .body(payload("GB82WEST12345698765432", "IBAN", "Al Tareq Trading LLC"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(200)
                .extract()
                .response();

        Response second = baseRequest()
                .body(payload("GB82WEST12345698765432", "IBAN", "Al Tareq Trading LLC"))
                .when()
                .post("/open-finance/v1/confirmation-of-payee/confirmation")
                .then()
                .statusCode(200)
                .time(Matchers.lessThan(500L))
                .extract()
                .response();

        org.assertj.core.api.Assertions.assertThat(first.getHeader("X-OF-Cache")).isEqualTo("MISS");
        org.assertj.core.api.Assertions.assertThat(second.getHeader("X-OF-Cache")).isEqualTo("HIT");
    }

    private RequestSpecification baseRequest() {
        return given().baseUri("http://localhost").port(port)
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP functional-token")
                .header("DPoP", "functional-proof")
                .header("X-FAPI-Interaction-ID", "ix-functional")
                .header("x-fapi-financial-id", "TPP-FUNCTIONAL");
    }

    private static String payload(String identification, String schemeName, String name) {
        return """
                {
                  "Data": {
                    "Identification": "%s",
                    "SchemeName": "%s",
                    "Name": "%s"
                  }
                }
                """.formatted(identification, schemeName, name);
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
            "com.enterprise.openfinance.uc03.application",
            "com.enterprise.openfinance.uc03.infrastructure"
    })
    static class TestApplication {
    }
}
