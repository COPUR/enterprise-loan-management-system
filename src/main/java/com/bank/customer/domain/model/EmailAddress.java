package com.bank.loan.loan.domain.customer;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing an email address.
 * Ensures proper email validation and immutability.
 */
public final class EmailAddress {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private final String value;
    
    private EmailAddress(String value) {
        this.value = validateEmail(value);
    }
    
    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
    
    private String validateEmail(String email) {
        Objects.requireNonNull(email, "Email cannot be null");
        String trimmed = email.trim().toLowerCase();
        
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        if (trimmed.length() > 254) {
            throw new IllegalArgumentException("Email cannot exceed 254 characters");
        }
        
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        // Banking-specific validation: no + symbols for security
        if (trimmed.contains("+")) {
            throw new IllegalArgumentException("Email addresses with + symbols are not supported");
        }
        
        return trimmed;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
    
    public String getLocalPart() {
        return value.substring(0, value.indexOf('@'));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailAddress that = (EmailAddress) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}