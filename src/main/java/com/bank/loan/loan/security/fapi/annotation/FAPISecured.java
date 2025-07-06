package com.bank.loan.loan.security.fapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FAPI 2.0 Security Annotation
 * 
 * Marks controllers/methods that require FAPI 2.0 compliance.
 * Enforces FAPI security headers and validation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FAPISecured {
    
    /**
     * Whether FAPI security headers are required
     */
    boolean requireHeaders() default true;
    
    /**
     * Whether FAPI interaction ID is required
     */
    boolean requireInteractionId() default true;
    
    /**
     * Whether customer IP address is required
     */
    boolean requireCustomerIp() default false;
    
    /**
     * Whether idempotency key is required
     */
    boolean requireIdempotencyKey() default false;
}