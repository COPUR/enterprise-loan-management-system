package com.bank.loanmanagement.security;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RateLimitingService {
    
    public Mono<Boolean> isAllowed(String clientId, String operation) {
        // Rate limiting logic would go here
        return Mono.just(true);
    }
    
    public Mono<Void> recordRequest(String clientId, String operation) {
        // Record request for rate limiting
        return Mono.empty();
    }
}