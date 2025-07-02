package com.bank.loanmanagement.loan.security.model;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenValidationService {
    public Mono<Boolean> validateJwt(String token) {
        return Mono.just(true);
    }
    
    public Jwt decodeAndValidateJWT(String token) {
        // Stub implementation for compilation
        return null;
    }
    
    public Jwt decodeJWT(String token) {
        // Stub implementation for compilation
        return null;
    }
}