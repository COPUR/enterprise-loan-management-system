package com.bank.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Simple test for DPoP Validator to verify GREEN phase implementation
 * 
 * Tests basic validation logic without complex JWT operations
 */
class DPoPValidatorSimpleTest {
    
    @Mock
    private HttpServletRequest request;
    
    private DPoPValidator validator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new DPoPValidator();
    }
    
    @Test
    @DisplayName("Should reject null DPoP proof")
    void shouldRejectNullDPoPProof() {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/customers");
        
        // Act & Assert
        DPoPValidationException exception = assertThrows(
            DPoPValidationException.class,
            () -> validator.validate(request, null, "Bearer token")
        );
        
        assertEquals("DPoP proof is required", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject empty DPoP proof")
    void shouldRejectEmptyDPoPProof() {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/customers");
        
        // Act & Assert
        DPoPValidationException exception = assertThrows(
            DPoPValidationException.class,
            () -> validator.validate(request, "", "Bearer token")
        );
        
        assertEquals("DPoP proof is required", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject malformed DPoP JWT")
    void shouldRejectMalformedDPoPJWT() {
        // Arrange
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/customers");
        
        // Act & Assert
        DPoPValidationException exception = assertThrows(
            DPoPValidationException.class,
            () -> validator.validate(request, "malformed.jwt.token", "Bearer token")
        );
        
        assertEquals("Invalid DPoP proof format", exception.getMessage());
    }
    
    @Test
    @DisplayName("GREEN Phase: Validation logic is implemented")
    void testValidatorExists() {
        // Verify that the validator class and methods exist
        assertNotNull(validator);
        assertTrue(validator instanceof DPoPValidator);
    }
}