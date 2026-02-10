package com.enterprise.openfinance.uc15.infrastructure.functional;

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
        classes = AtmDataUatTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
)
class AtmDataUatTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void shouldServeAtmDirectoryAndLocationFilter() {
        request()
                .when()
                .get("/open-finance/v1/atms")
                .then()
                .statusCode(200)
                .header("X-OF-Cache", notNullValue())
                .body("Data.ATM", notNullValue());

        request()
                .queryParam("lat", "25.2048")
                .queryParam("long", "55.2708")
                .queryParam("radius", "2")
                .when()
                .get("/open-finance/v1/atms")
                .then()
                .statusCode(200)
                .body("Data.ATM.size()", equalTo(1))
                .body("Data.ATM[0].AtmId", equalTo("ATM-001"));
    }

    @Test
    void shouldRejectInvalidAuthorization() {
        given()
                .accept("application/json")
                .header("X-FAPI-Interaction-ID", "ix-uc15-uat-err")
                .header("Authorization", "Basic invalid")
                .when()
                .get("/open-finance/v1/atms")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"));
    }

    @Test
    void shouldProvideCacheHitAndSupportEtagRevalidation() {
        Response first = request()
                .when()
                .get("/open-finance/v1/atms")
                .then()
                .statusCode(200)
                .header("X-OF-Cache", equalTo("MISS"))
                .header("ETag", notNullValue())
                .extract()
                .response();

        String etag = first.getHeader("ETag");

        request()
                .when()
                .get("/open-finance/v1/atms")
                .then()
                .statusCode(200)
                .header("X-OF-Cache", equalTo("HIT"));

        request()
                .header("If-None-Match", etag)
                .when()
                .get("/open-finance/v1/atms")
                .then()
                .statusCode(304);
    }

    private RequestSpecification request() {
        return given()
                .accept("application/json")
                .header("X-FAPI-Interaction-ID", "ix-uc15-uat")
                .header("Authorization", "Bearer public-client-token");
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
            "com.enterprise.openfinance.uc15.application",
            "com.enterprise.openfinance.uc15.infrastructure"
    })
    static class TestApplication {
    }
}
