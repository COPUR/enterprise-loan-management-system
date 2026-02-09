package com.enterprise.openfinance.uc03.infrastructure.rest;

import com.enterprise.openfinance.uc03.infrastructure.rest.dto.ConfirmationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ConfirmationExceptionHandlerTest {

    private final ConfirmationExceptionHandler handler = new ConfirmationExceptionHandler();

    @Test
    void shouldHandleBadRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn("ix-1");

        ResponseEntity<ConfirmationErrorResponse> response = handler.handleBadRequest(
                new IllegalArgumentException("invalid IBAN"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("invalid IBAN");
    }

    @Test
    void shouldHandleConflict() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn("ix-2");

        ResponseEntity<ConfirmationErrorResponse> response = handler.handleConflict(
                new IllegalStateException("conflict"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
    }

    @Test
    void shouldHandleUnexpected() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn("ix-3");

        ResponseEntity<ConfirmationErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Unexpected error occurred");
    }
}
