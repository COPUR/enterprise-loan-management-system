package com.enterprise.openfinance.consentauthorization.infrastructure.functional;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag("functional")
@Tag("e2e")
@SpringBootTest(
        classes = ConsentManagementUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
                "openfinance.internal.secrets.storage=memory"
        }
)
class ConsentManagementUatTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-02-09T10:00:00Z");

    @LocalServerPort
    private int port;

    @Autowired
    private MutableClock mutableClock;

    @BeforeEach
    void setUp() {
        mutableClock.setInstant(INITIAL_TIME);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldCompleteConsentLifecycleForUserJourney() {
        Response created = baseRequest()
                .body("""
                        {
                          "customerId": "CUST-UAT-1001",
                          "participantId": "TPP-UAT-01",
                          "scopes": ["ReadAccounts", "ReadBalances"],
                          "purpose": "Budgeting with TPP",
                          "expiresAt": "%s"
                        }
                        """.formatted(mutableClock.instant().plus(3, ChronoUnit.DAYS)))
                .when()
                .post("/open-finance/v1/consents")
                .then()
                .statusCode(201)
                .body("consentId", notNullValue())
                .body("status", equalTo("PENDING"))
                .extract()
                .response();

        String consentId = created.jsonPath().getString("consentId");

        baseRequest()
                .when()
                .post("/open-finance/v1/consents/{consentId}/authorize", consentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("AUTHORIZED"))
                .body("active", equalTo(true));

        baseRequest()
                .body("""
                        {"reason":"Customer ended sharing"}
                        """)
                .when()
                .patch("/open-finance/v1/consents/{consentId}/revoke", consentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REVOKED"))
                .body("revocationReason", equalTo("Customer ended sharing"))
                .body("active", equalTo(false));

        baseRequest()
                .when()
                .get("/open-finance/v1/consents/{consentId}", consentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REVOKED"))
                .body("active", equalTo(false));
    }

    @Test
    void shouldRejectInvalidPermissionAsBadRequest() {
        baseRequest()
                .body("""
                        {
                          "customerId": "CUST-UAT-1002",
                          "participantId": "TPP-UAT-02",
                          "scopes": ["ReadAdminData"],
                          "purpose": "Invalid permission validation",
                          "expiresAt": "%s"
                        }
                        """.formatted(mutableClock.instant().plus(1, ChronoUnit.DAYS)))
                .when()
                .post("/open-finance/v1/consents")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"))
                .body("message", containsString("invalid scope"));
    }

    @Test
    void shouldReturnConflictForExpiredConsentAuthorizationAndPersistExpiredState() {
        Response created = baseRequest()
                .body("""
                        {
                          "customerId": "CUST-UAT-1003",
                          "participantId": "TPP-UAT-03",
                          "scopes": ["ReadAccounts"],
                          "purpose": "Short lived consent",
                          "expiresAt": "%s"
                        }
                        """.formatted(mutableClock.instant().plus(30, ChronoUnit.SECONDS)))
                .when()
                .post("/open-finance/v1/consents")
                .then()
                .statusCode(201)
                .extract()
                .response();

        String consentId = created.jsonPath().getString("consentId");
        mutableClock.setInstant(mutableClock.instant().plus(31, ChronoUnit.SECONDS));

        baseRequest()
                .when()
                .post("/open-finance/v1/consents/{consentId}/authorize", consentId)
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT"))
                .body("message", containsString("expired"));

        baseRequest()
                .when()
                .get("/open-finance/v1/consents/{consentId}", consentId)
                .then()
                .statusCode(200)
                .body("status", equalTo("EXPIRED"))
                .body("active", equalTo(false));
    }

    @Test
    void shouldCompletePkceAuthorizationCodeJourney() throws Exception {
        Response created = baseRequest()
                .body("""
                        {
                          "customerId": "CUST-UAT-PKCE",
                          "participantId": "TPP-UAT-PKCE",
                          "scopes": ["ReadAccounts", "ReadBalances"],
                          "purpose": "PKCE authorization flow",
                          "expiresAt": "%s"
                        }
                        """.formatted(mutableClock.instant().plus(2, ChronoUnit.DAYS)))
                .when()
                .post("/open-finance/v1/consents")
                .then()
                .statusCode(201)
                .extract()
                .response();

        String consentId = created.jsonPath().getString("consentId");

        baseRequest()
                .when()
                .post("/open-finance/v1/consents/{consentId}/authorize", consentId)
                .then()
                .statusCode(200);

        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))
        );

        Response authorizeResponse = given()
                .accept("*/*")
                .redirects().follow(false)
                .queryParam("response_type", "code")
                .queryParam("client_id", "uat-mobile-client")
                .queryParam("redirect_uri", "https://tpp.example/callback")
                .queryParam("scope", "ReadAccounts ReadBalances")
                .queryParam("state", "uat-state")
                .queryParam("consent_id", consentId)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .when()
                .get("/oauth2/authorize")
                .then()
                .statusCode(302)
                .extract()
                .response();

        String location = authorizeResponse.getHeader("Location");
        String code = location.substring(location.indexOf("code=") + 5, location.indexOf("&state="));

        given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .formParam("grant_type", "authorization_code")
                .formParam("code", code)
                .formParam("code_verifier", verifier)
                .formParam("client_id", "uat-mobile-client")
                .formParam("redirect_uri", "https://tpp.example/callback")
                .when()
                .post("/oauth2/token")
                .then()
                .statusCode(200)
                .body("access_token", notNullValue())
                .body("refresh_token", notNullValue())
                .body("token_type", equalTo("Bearer"));
    }

    private RequestSpecification baseRequest() {
        return given()
                .contentType("application/json")
                .accept("application/json")
                .header("Authorization", "DPoP uat-token")
                .header("DPoP", "uat-dpop-proof")
                .header("X-FAPI-Interaction-ID", UUID.randomUUID().toString());
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
            "com.enterprise.openfinance.consentauthorization.application",
            "com.enterprise.openfinance.consentauthorization.infrastructure"
    })
    static class TestApplication {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(INITIAL_TIME);
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }

        void setInstant(Instant next) {
            current.set(next);
        }
    }
}
