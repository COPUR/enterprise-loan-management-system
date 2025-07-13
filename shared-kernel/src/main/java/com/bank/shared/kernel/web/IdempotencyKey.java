package com.bank.shared.kernel.web;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Validation annotation for Idempotency Keys
 * 
 * Ensures idempotency keys meet FAPI 2.0 and banking security requirements:
 * - Length between 16-64 characters
 * - Alphanumeric characters, hyphens, and underscores only
 * - Must be unique per operation
 * - UUID format recommended but not required
 */
@Documented
@Constraint(validatedBy = IdempotencyKey.IdempotencyKeyValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotencyKey {
    
    String message() default "Invalid idempotency key format. Must be 16-64 characters, alphanumeric with hyphens/underscores allowed";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Validator implementation for idempotency keys
     */
    class IdempotencyKeyValidator implements ConstraintValidator<IdempotencyKey, String> {
        
        private static final Pattern IDEMPOTENCY_KEY_PATTERN = 
            Pattern.compile("^[a-zA-Z0-9-_]{16,64}$");
        
        @Override
        public void initialize(IdempotencyKey constraintAnnotation) {
            // No initialization needed
        }
        
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return false;
            }
            
            // Check format
            if (!IDEMPOTENCY_KEY_PATTERN.matcher(value).matches()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Idempotency key must be 16-64 characters long and contain only " +
                    "alphanumeric characters, hyphens, and underscores")
                    .addConstraintViolation();
                return false;
            }
            
            // Additional security checks
            if (containsSequentialCharacters(value)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    "Idempotency key should not contain obvious sequential patterns")
                    .addConstraintViolation();
                return false;
            }
            
            return true;
        }
        
        /**
         * Check for sequential characters that might indicate a weak key
         */
        private boolean containsSequentialCharacters(String value) {
            // Check for simple sequential patterns
            for (int i = 0; i < value.length() - 2; i++) {
                char c1 = value.charAt(i);
                char c2 = value.charAt(i + 1);
                char c3 = value.charAt(i + 2);
                
                // Check for ascending sequence (123, abc)
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true;
                }
                
                // Check for descending sequence (321, cba)
                if (c2 == c1 - 1 && c3 == c2 - 1) {
                    return true;
                }
                
                // Check for repeated characters (aaa, 111)
                if (c1 == c2 && c2 == c3) {
                    return true;
                }
            }
            
            return false;
        }
    }
}