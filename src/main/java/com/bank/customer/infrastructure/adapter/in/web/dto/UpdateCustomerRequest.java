package com.bank.customer.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for updating customer information via REST API.
 */
public record UpdateCustomerRequest(
    
    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName,
    
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName,
    
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    String email,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in valid international format")
    String phoneNumber
) {}