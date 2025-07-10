package com.loanmanagement.shared.infrastructure.config;

import com.loanmanagement.shared.application.port.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Dependency Inversion Configuration
 * Validates that all abstractions are properly wired
 */
@SpringBootTest
@TestPropertySource(properties = {
        "loan.management.dependency-inversion.enabled=true"
})
@DisplayName("Dependency Inversion Configuration Tests")
class DependencyInversionTest {
    
    @Autowired(required = false)
    private TransactionManager transactionManager;
    
    @Autowired(required = false)
    private LoggingFactory loggingFactory;
    
    @Autowired(required = false)
    private TimeProvider timeProvider;
    
    @Autowired(required = false)
    private ValidationPort validationPort;
    
    @Autowired(required = false)
    private HttpClientPort httpClientPort;
    
    @Test
    @DisplayName("Should wire transaction manager abstraction")
    void shouldWireTransactionManager() {
        assertNotNull(transactionManager, "TransactionManager should be wired");
        
        // Test basic functionality
        String result = transactionManager.executeInTransaction(() -> "test-result");
        assertEquals("test-result", result);
    }
    
    @Test
    @DisplayName("Should wire logging factory abstraction")
    void shouldWireLoggingFactory() {
        assertNotNull(loggingFactory, "LoggingFactory should be wired");
        
        // Test logger creation
        LoggingPort logger = loggingFactory.getLogger(DependencyInversionTest.class);
        assertNotNull(logger, "Logger should be created");
        
        // Test logging capabilities
        assertTrue(logger.isInfoEnabled() || logger.isDebugEnabled(), 
                "At least one logging level should be enabled");
    }
    
    @Test
    @DisplayName("Should wire time provider abstraction")
    void shouldWireTimeProvider() {
        assertNotNull(timeProvider, "TimeProvider should be wired");
        
        // Test time operations
        assertNotNull(timeProvider.now(), "Should provide current time");
        assertNotNull(timeProvider.today(), "Should provide current date");
        assertTrue(timeProvider.currentTimeMillis() > 0, "Should provide positive timestamp");
    }
    
    @Test
    @DisplayName("Should wire validation port abstraction")
    void shouldWireValidationPort() {
        assertNotNull(validationPort, "ValidationPort should be wired");
        
        // Test basic validation
        TestValidationObject testObject = new TestValidationObject("valid");
        ValidationPort.ValidationResult result = validationPort.validate(testObject);
        assertNotNull(result, "Validation result should not be null");
        assertTrue(result.valid(), "Valid object should pass validation");
    }
    
    @Test
    @DisplayName("Should wire HTTP client port abstraction")
    void shouldWireHttpClientPort() {
        assertNotNull(httpClientPort, "HttpClientPort should be wired");
        
        // Note: We don't test actual HTTP calls here to avoid external dependencies
        // This just verifies the bean is properly wired
    }
    
    @Test
    @DisplayName("Should support dependency inversion principles")
    void shouldSupportDependencyInversionPrinciples() {
        // Verify that we're working with abstractions, not concrete implementations
        assertFalse(transactionManager.getClass().getSimpleName().contains("Spring"),
                "Should work with abstraction, not Spring-specific class directly");
        
        assertFalse(loggingFactory.getClass().getSimpleName().contains("Slf4j"),
                "Should work with abstraction, not SLF4J-specific class directly");
        
        // Verify abstractions can be used without framework knowledge
        LoggingPort logger = loggingFactory.getLogger("test.logger");
        logger.info("Test message from dependency inversion test");
        
        assertDoesNotThrow(() -> {
            timeProvider.now();
            transactionManager.executeWithoutTransaction(() -> {
                // No-op operation
            });
        }, "Should work without throwing exceptions");
    }
    
    // Test helper class for validation testing
    private static class TestValidationObject {
        private final String value;
        
        public TestValidationObject(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}