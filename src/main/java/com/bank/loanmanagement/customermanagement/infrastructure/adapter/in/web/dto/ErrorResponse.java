package com.bank.loanmanagement.customermanagement.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

/**
 * DTO for error responses in REST API.
 */
public record ErrorResponse(
    String code,
    String message,
    LocalDateTime timestamp
) {
    public ErrorResponse(String code, String message) {
        this(code, message, LocalDateTime.now());
    }
}