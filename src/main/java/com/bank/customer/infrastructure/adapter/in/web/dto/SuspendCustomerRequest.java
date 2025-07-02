package com.bank.customer.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for suspending customer via REST API.
 */
public record SuspendCustomerRequest(
    
    @NotBlank(message = "Suspension reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    String reason
) {}