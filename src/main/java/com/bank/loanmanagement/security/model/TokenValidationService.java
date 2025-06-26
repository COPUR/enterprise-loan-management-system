package com.bank.loanmanagement.security.model;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenValidationService {
    public Mono<Boolean> validateJwt(String token) {
        return Mono.just(true);
    }
}