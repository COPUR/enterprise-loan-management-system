package com.enterprise.openfinance.paymentinitiation.infrastructure.rest;

import com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto.PaymentErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class PaymentExceptionHandlerTest {

    private final PaymentExceptionHandler handler = new PaymentExceptionHandler();

    @Test
    void shouldHandleBadRequest() {
        ResponseEntity<PaymentErrorResponse> response = handler.handleBadRequest(
                new IllegalArgumentException("invalid payload"),
                request("ix-001")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldHandleConflictForIdempotency() {
        ResponseEntity<PaymentErrorResponse> response = handler.handleIllegalState(
                new IllegalStateException("Idempotency conflict"),
                request("ix-002")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
    }

    @Test
    void shouldHandleBusinessRuleViolationAsUnprocessable() {
        ResponseEntity<PaymentErrorResponse> response = handler.handleIllegalState(
                new IllegalStateException("Insufficient funds"),
                request("ix-003")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("BUSINESS_RULE_VIOLATION");
    }

    @Test
    void shouldHandleUnexpectedError() {
        ResponseEntity<PaymentErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"),
                request("ix-004")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }

    private static HttpServletRequest request(String interactionId) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn(interactionId);
        return request;
    }
}
