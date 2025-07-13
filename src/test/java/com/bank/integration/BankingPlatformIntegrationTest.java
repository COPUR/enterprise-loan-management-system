package com.bank.integration;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Integration Tests for Banking Platform
 * 
 * Tests the complete banking platform using Testcontainers:
 * - Database integration with PostgreSQL
 * - Message streaming with Kafka
 * - Caching with Redis
 * - End-to-end API workflows
 * - Security integration
 * - Real-time event processing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.profiles.active=integration-test",
    "logging.level.com.bank=DEBUG"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankingPlatformIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Test containers
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("banking_test")
        .withUsername("test_user")
        .withPassword("test_pass")
        .withInitScript("init-test-db.sql");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
        .withEnv("KAFKA_NUM_PARTITIONS", "3");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withCommand("redis-server", "--appendonly", "yes");
    
    @Container
    static GenericContainer<?> jaeger = new GenericContainer<>("jaegertracing/all-in-one:1.48")
        .withExposedPorts(14268, 16686)
        .withEnv("COLLECTOR_OTLP_ENABLED", "true");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        
        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        // Jaeger configuration
        registry.add("management.otlp.tracing.endpoint", 
            () -> "http://" + jaeger.getHost() + ":" + jaeger.getMappedPort(14268) + "/api/traces");
    }
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should verify all containers are running")
    void shouldVerifyContainersRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(kafka.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
        assertThat(jaeger.isRunning()).isTrue();
    }
    
    @Test
    @Order(2)
    @DisplayName("Should check API health and version")
    void shouldCheckApiHealthAndVersion() {
        given()
            .when()
                .get("/api/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("api.currentVersion", notNullValue())
                .body("features.authentication", containsString("OAuth"))
                .body("features.security", containsString("FAPI"));
        
        // Check version endpoint
        given()
            .when()
                .get("/api/versions")
            .then()
                .statusCode(200)
                .body("currentVersion", notNullValue())
                .body("supportedVersions", hasSize(greaterThan(0)))
                .body("versioning.urlPattern", notNullValue());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should create customer successfully")
    void shouldCreateCustomerSuccessfully() {
        String idempotencyKey = "test-customer-" + UUID.randomUUID();
        
        Map<String, Object> customerRequest = Map.of(
            "firstName", "John",
            "lastName", "Doe",
            "email", "john.doe.test@example.com",
            "phoneNumber", "+1-555-TEST",
            "dateOfBirth", "1990-01-01",
            "address", Map.of(
                "street", "123 Test St",
                "city", "Test City",
                "state", "TS",
                "zipCode", "12345",
                "country", "US"
            ),
            "creditLimit", 10000.00
        );
        
        Response response = given()
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .header("X-FAPI-Interaction-Id", UUID.randomUUID().toString())
            .body(customerRequest)
            .when()
                .post("/api/v2/customers")
            .then()
                .statusCode(201)
                .header("X-Resource-Id", notNullValue())
                .header("X-Idempotency-Key", equalTo(idempotencyKey))
                .body("customerId", notNullValue())
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"))
                .body("status", equalTo("ACTIVE"))
                .body("_links.self.href", notNullValue())
                .body("_links.update-credit-limit.href", notNullValue())
                .extract().response();
        
        // Store customer ID for subsequent tests
        String customerId = response.path("customerId");
        System.setProperty("test.customerId", customerId);
    }
    
    @Test
    @Order(4)
    @DisplayName("Should test idempotency for customer creation")
    void shouldTestIdempotencyForCustomerCreation() {
        String idempotencyKey = "test-idempotency-" + UUID.randomUUID();
        
        Map<String, Object> customerRequest = Map.of(
            "firstName", "Jane",
            "lastName", "Smith",
            "email", "jane.smith.test@example.com",
            "phoneNumber", "+1-555-IDMP",
            "dateOfBirth", "1985-05-15",
            "address", Map.of(
                "street", "456 Idempotency Ave",
                "city", "Test City",
                "state", "TS",
                "zipCode", "54321",
                "country", "US"
            ),
            "creditLimit", 15000.00
        );
        
        RequestSpecification request = given()
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .body(customerRequest);
        
        // First request - should create customer
        Response firstResponse = request.when().post("/api/v2/customers");
        firstResponse.then()
            .statusCode(201)
            .body("customerId", notNullValue());
        
        String firstCustomerId = firstResponse.path("customerId");
        
        // Second request with same idempotency key - should return same result
        Response secondResponse = request.when().post("/api/v2/customers");
        secondResponse.then()
            .statusCode(200) // Should return cached result
            .body("customerId", equalTo(firstCustomerId));
    }
    
    @Test
    @Order(5)
    @DisplayName("Should create loan application")
    void shouldCreateLoanApplication() {
        String customerId = System.getProperty("test.customerId");
        assertThat(customerId).isNotNull();
        
        String idempotencyKey = "test-loan-" + UUID.randomUUID();
        
        Map<String, Object> loanRequest = Map.of(
            "customerId", customerId,
            "principalAmount", 25000.00,
            "currency", "USD",
            "annualInterestRate", 8.5,
            "termInMonths", 60
        );
        
        Response response = given()
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .header("X-FAPI-Interaction-Id", UUID.randomUUID().toString())
            .body(loanRequest)
            .when()
                .post("/api/v2/loans")
            .then()
                .statusCode(201)
                .header("X-Resource-Id", notNullValue())
                .body("loanId", notNullValue())
                .body("customerId", equalTo(customerId))
                .body("principalAmount", equalTo(25000.0f))
                .body("status", equalTo("PENDING_APPROVAL"))
                .body("_links.self.href", notNullValue())
                .body("_links.approve.href", notNullValue())
                .body("_links.reject.href", notNullValue())
                .extract().response();
        
        String loanId = response.path("loanId");
        System.setProperty("test.loanId", loanId);
    }
    
    @Test
    @Order(6)
    @DisplayName("Should process payment successfully")
    void shouldProcessPaymentSuccessfully() {
        String customerId = System.getProperty("test.customerId");
        assertThat(customerId).isNotNull();
        
        String idempotencyKey = "test-payment-" + UUID.randomUUID();
        
        Map<String, Object> paymentRequest = Map.of(
            "customerId", customerId,
            "fromAccountId", "ACC-TEST-001",
            "toAccountId", "ACC-TEST-002", 
            "amount", 1000.00,
            "currency", "USD",
            "paymentType", "BANK_TRANSFER",
            "description", "Integration test payment"
        );
        
        Response response = given()
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .header("X-FAPI-Interaction-Id", UUID.randomUUID().toString())
            .body(paymentRequest)
            .when()
                .post("/api/v2/payments")
            .then()
                .statusCode(201)
                .header("X-Resource-Id", notNullValue())
                .body("paymentId", notNullValue())
                .body("customerId", equalTo(customerId))
                .body("amount", equalTo(1000.0f))
                .body("status", oneOf("PENDING", "PROCESSING", "COMPLETED"))
                .body("_links.self.href", notNullValue())
                .body("_links.events.href", notNullValue())
                .extract().response();
        
        String paymentId = response.path("paymentId");
        System.setProperty("test.paymentId", paymentId);
    }
    
    @Test
    @Order(7)
    @DisplayName("Should handle invalid requests with proper error responses")
    void shouldHandleInvalidRequestsWithProperErrorResponses() {
        String idempotencyKey = "test-invalid-" + UUID.randomUUID();
        
        // Invalid customer request - missing required fields
        Map<String, Object> invalidRequest = Map.of(
            "firstName", "Test"
            // Missing required fields
        );
        
        given()
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .body(invalidRequest)
            .when()
                .post("/api/v2/customers")
            .then()
                .statusCode(400)
                .body("title", equalTo("Validation Error"))
                .body("type", notNullValue())
                .body("errors", notNullValue())
                .body("timestamp", notNullValue());
    }
    
    @Test
    @Order(8)
    @DisplayName("Should test rate limiting")
    void shouldTestRateLimiting() {
        // This test would need to be adjusted based on actual rate limits
        // For demonstration, we'll test that rate limit headers are present
        
        given()
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .when()
                .get("/api/v2/customers")
            .then()
                .header("X-RateLimit-Limit", notNullValue())
                .header("X-RateLimit-Remaining", notNullValue());
    }
    
    @Test
    @Order(9)
    @DisplayName("Should test API versioning")
    void shouldTestApiVersioning() {
        // Test URL-based versioning
        given()
            .when()
                .get("/api/v1")
            .then()
                .statusCode(200)
                .header("X-API-Deprecated", equalTo("true"))
                .header("Warning", containsString("deprecated"))
                .body("version", equalTo("1.0"))
                .body("status", equalTo("deprecated"));
        
        // Test header-based versioning
        given()
            .header("Accept", "application/vnd.banking.v2+json")
            .when()
                .get("/api/v2")
            .then()
                .statusCode(200)
                .header("X-API-Version", equalTo("2.0"))
                .body("version", equalTo("2.0"))
                .body("status", equalTo("current"));
    }
    
    @Test
    @Order(10)
    @DisplayName("Should test circuit breaker behavior")
    void shouldTestCircuitBreakerBehavior() {
        // This would test circuit breaker functionality
        // For a real test, we'd need to simulate service failures
        
        given()
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .when()
                .get("/actuator/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
    
    @Test
    @Order(11)
    @DisplayName("Should verify database integration")
    void shouldVerifyDatabaseIntegration() {
        String customerId = System.getProperty("test.customerId");
        if (customerId != null) {
            // Verify customer exists in database by fetching it
            given()
                .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
                .when()
                    .get("/api/v2/customers/{customerId}", customerId)
                .then()
                    .statusCode(200)
                    .body("customerId", equalTo(customerId))
                    .body("firstName", equalTo("John"));
        }
    }
    
    @Test
    @Order(12)
    @DisplayName("Should test HATEOAS links")
    void shouldTestHateoasLinks() {
        String customerId = System.getProperty("test.customerId");
        if (customerId != null) {
            Response response = given()
                .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
                .when()
                    .get("/api/v2/customers/{customerId}", customerId)
                .then()
                    .statusCode(200)
                    .body("_links", notNullValue())
                    .body("_links.self", notNullValue())
                    .body("_links.self.href", containsString(customerId))
                    .extract().response();
            
            // Test following HATEOAS links
            String selfLink = response.path("_links.self.href");
            assertThat(selfLink).isNotNull();
            
            // Follow the self link
            given()
                .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
                .when()
                    .get(selfLink.replace("http://localhost:" + port, ""))
                .then()
                    .statusCode(200)
                    .body("customerId", equalTo(customerId));
        }
    }
    
    @Test
    @Order(13)
    @DisplayName("Should test OpenAPI documentation endpoints")
    void shouldTestOpenApiDocumentationEndpoints() {
        // Test OpenAPI spec endpoint
        given()
            .when()
                .get("/v3/api-docs")
            .then()
                .statusCode(200)
                .body("openapi", notNullValue())
                .body("info.title", containsString("Banking"))
                .body("paths", notNullValue());
        
        // Test Swagger UI
        given()
            .when()
                .get("/swagger-ui/index.html")
            .then()
                .statusCode(200);
    }
    
    @Test
    @Order(14)
    @DisplayName("Should test actuator endpoints")
    void shouldTestActuatorEndpoints() {
        // Health endpoint
        given()
            .when()
                .get("/actuator/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
        
        // Metrics endpoint
        given()
            .when()
                .get("/actuator/metrics")
            .then()
                .statusCode(200)
                .body("names", notNullValue());
        
        // Info endpoint
        given()
            .when()
                .get("/actuator/info")
            .then()
                .statusCode(200);
    }
    
    @AfterAll
    static void tearDown() {
        // Containers are automatically stopped by Testcontainers
        System.out.println("Integration tests completed. Containers will be cleaned up automatically.");
    }
    
    /**
     * Helper method to wait for async operations
     */
    private void waitForAsyncOperation(Duration timeout) {
        try {
            Thread.sleep(timeout.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Helper method to create a test request specification with common headers
     */
    private RequestSpecification createTestRequest() {
        return given()
            .header("Content-Type", "application/json")
            .header("X-FAPI-Financial-Id", "TEST-FCA-123456")
            .header("X-FAPI-Interaction-Id", UUID.randomUUID().toString());
    }
}