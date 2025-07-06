package com.bank.loan.loan.security.dpop;

import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.exception.TokenBindingMismatchException;
import com.bank.loan.loan.security.dpop.service.DPoPNonceService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DPoPBasicImplementationTest {
    
    @Test
    public void testInvalidDPoPProofExceptionCreation() {
        InvalidDPoPProofException exception = new InvalidDPoPProofException("Test error");
        
        assertThat(exception.getMessage()).isEqualTo("Test error");
        assertThat(exception.getErrorCode()).isEqualTo("invalid_dpop_proof");
        assertThat(exception.getErrorDescription()).isEqualTo("Test error");
    }
    
    @Test
    public void testTokenBindingMismatchExceptionCreation() {
        TokenBindingMismatchException exception = new TokenBindingMismatchException("Binding error");
        
        assertThat(exception.getMessage()).isEqualTo("Binding error");
        assertThat(exception.getErrorCode()).isEqualTo("token_binding_mismatch");
        assertThat(exception.getErrorDescription()).isEqualTo("Binding error");
    }
    
    @Test
    public void testDPoPNonceServiceCreation() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> mockRedisTemplate = mock(RedisTemplate.class);
        
        DPoPNonceService nonceService = new DPoPNonceService(mockRedisTemplate);
        
        assertThat(nonceService).isNotNull();
    }
    
    @Test
    public void testDPoPExceptionWithCustomErrorCode() {
        InvalidDPoPProofException exception = new InvalidDPoPProofException("use_dpop_nonce", "Nonce required");
        
        assertThat(exception.getErrorCode()).isEqualTo("use_dpop_nonce");
        assertThat(exception.getErrorDescription()).isEqualTo("Nonce required");
    }
}