package com.bank.loan.loan.security.dpop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DPoP Security Annotation
 * 
 * Marks controllers/methods that require DPoP-bound access tokens.
 * Used by security interceptors to enforce DPoP validation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DPoPSecured {
    
    /**
     * Whether DPoP validation is required for this endpoint
     */
    boolean required() default true;
    
    /**
     * Additional security requirements
     */
    String[] requirements() default {};
}