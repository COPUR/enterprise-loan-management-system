package com.amanahfi.platform.integration;

import com.amanahfi.platform.AmanahFiPlatformApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Abstract base class for integration tests in the AmanahFi Platform.
 * 
 * This class sets up the complete test environment including:
 * - PostgreSQL database
 * - Redis cache
 * - Kafka messaging
 * - Mock external services
 * 
 * All integration tests should extend this class to ensure consistent
 * test environment setup for Islamic Finance and CBDC functionality.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = AmanahFiPlatformApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebMvc
@ActiveProfiles({"integration-test", "islamic-finance", "cbdc"})
@Testcontainers
@Transactional
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // PostgreSQL container for database testing
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("amanahfi_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("test-data.sql")
            .withStartupTimeout(Duration.ofMinutes(2));

    // Redis container for cache testing
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "test_password")
            .withStartupTimeout(Duration.ofMinutes(1));

    // Kafka container for messaging testing
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withStartupTimeout(Duration.ofMinutes(2));

    // Mock regulatory APIs container (WireMock)
    @Container
    static GenericContainer<?> mockRegulatoryApis = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.0.0"))
            .withExposedPorts(8080)
            .withCommand("--global-response-templating", "--verbose")
            .withFileSystemBind("src/test/resources/wiremock", "/home/wiremock")
            .withStartupTimeout(Duration.ofMinutes(1));

    // Mock Corda network container for CBDC testing
    @Container
    static GenericContainer<?> mockCordaNetwork = new GenericContainer<>(DockerImageName.parse("wiremock/wiremock:3.0.0"))
            .withExposedPorts(8080)
            .withCommand("--global-response-templating", "--verbose", "--port", "8080")
            .withFileSystemBind("src/test/resources/corda-mock", "/home/wiremock")
            .withStartupTimeout(Duration.ofMinutes(1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "test_password");

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "amanahfi-test");

        // Mock regulatory APIs configuration
        registry.add("regulatory.apis.cbuae.base-url", 
            () -> "http://localhost:" + mockRegulatoryApis.getFirstMappedPort());
        registry.add("regulatory.apis.vara.base-url", 
            () -> "http://localhost:" + mockRegulatoryApis.getFirstMappedPort());
        registry.add("regulatory.apis.hsa.base-url", 
            () -> "http://localhost:" + mockRegulatoryApis.getFirstMappedPort());

        // Mock Corda network configuration
        registry.add("cbdc.corda.network.endpoint", 
            () -> "http://localhost:" + mockCordaNetwork.getFirstMappedPort());
        registry.add("cbdc.corda.network.timeout", () -> "30000");

        // Test-specific configurations
        registry.add("management.endpoints.web.exposure.include", () -> "*");
        registry.add("logging.level.com.amanahfi", () -> "DEBUG");
        registry.add("spring.jpa.show-sql", () -> "true");
        
        // Islamic Finance test configuration
        registry.add("islamic-finance.sharia-compliance.strict-mode", () -> "true");
        registry.add("islamic-finance.profit-margin.max-limit", () -> "0.30");
        
        // CBDC test configuration
        registry.add("cbdc.digital-dirham.test-mode", () -> "true");
        registry.add("cbdc.network.mock-enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        // Common setup for all integration tests
        configureTestEnvironment();
    }

    /**
     * Configure test environment before each test
     */
    protected void configureTestEnvironment() {
        // Override in subclasses for specific test setup
    }

    /**
     * Get the base URL for REST API calls
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Get the base URL for Islamic Finance API calls
     */
    protected String getIslamicFinanceApiUrl() {
        return getBaseUrl() + "/api/v1/islamic-finance";
    }

    /**
     * Get the base URL for CBDC API calls
     */
    protected String getCbdcApiUrl() {
        return getBaseUrl() + "/api/v1/cbdc";
    }

    /**
     * Get the base URL for regulatory compliance API calls
     */
    protected String getRegulatoryApiUrl() {
        return getBaseUrl() + "/api/v1/regulatory";
    }

    /**
     * Wait for the application to be ready
     */
    protected void waitForApplicationReady() throws InterruptedException {
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                restTemplate.getForEntity(getBaseUrl() + "/actuator/health", String.class);
                return; // Application is ready
            } catch (Exception e) {
                Thread.sleep(1000);
                attempt++;
            }
        }
        
        throw new RuntimeException("Application failed to start within timeout");
    }

    /**
     * Create test authentication token for API calls
     */
    protected String createTestAuthToken() {
        // In real implementation, this would create a proper JWT token
        // For testing, we'll use a mock token
        return "Bearer test-token-islamic-finance-cbdc";
    }

    /**
     * Verify Islamic Finance compliance for test data
     */
    protected void verifyIslamicFinanceCompliance(Object testData) {
        // Add Islamic Finance compliance verification logic
        // This would validate that test data adheres to Sharia principles
    }

    /**
     * Verify CBDC compliance for test data
     */
    protected void verifyCbdcCompliance(Object testData) {
        // Add CBDC compliance verification logic
        // This would validate that test data adheres to CBUAE regulations
    }

    /**
     * Clean up test data after each test
     */
    protected void cleanupTestData() {
        // Override in subclasses for specific cleanup
    }
}