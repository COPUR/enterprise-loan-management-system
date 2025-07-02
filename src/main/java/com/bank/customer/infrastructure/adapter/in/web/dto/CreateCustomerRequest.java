package com.bank.customer.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO for creating a new customer via REST API.
 */
public record CreateCustomerRequest(
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    String email,
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid international format")
    String phoneNumber,
    
    @NotNull(message = "Initial credit limit is required")
    @DecimalMin(value = "1000.0", message = "Initial credit limit must be at least $1,000")
    @DecimalMax(value = "10000000.0", message = "Initial credit limit cannot exceed $10,000,000")
    @Digits(integer = 10, fraction = 2, message = "Invalid credit limit format")
    BigDecimal initialCreditLimit
) {}