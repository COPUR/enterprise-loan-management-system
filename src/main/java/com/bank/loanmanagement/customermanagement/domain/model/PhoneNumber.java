package com.bank.loanmanagement.customermanagement.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a phone number.
 * Handles validation and formatting for international phone numbers.
 */
public final class PhoneNumber {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"  // E.164 format
    );
    
    private final String value;
    
    private PhoneNumber(String value) {
        this.value = validatePhoneNumber(value);
    }
    
    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }
    
    private String validatePhoneNumber(String phone) {
        Objects.requireNonNull(phone, "Phone number cannot be null");
        
        // Remove all non-digit characters except +
        String cleaned = phone.replaceAll("[^+\\d]", "");
        
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + phone);
        }
        
        return cleaned;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getFormatted() {
        if (value.startsWith("+1") && value.length() == 12) {
            // US format: +1 (XXX) XXX-XXXX
            return String.format("+1 (%s) %s-%s",
                value.substring(2, 5),
                value.substring(5, 8),
                value.substring(8));
        }
        return value; // Return as-is for other formats
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
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