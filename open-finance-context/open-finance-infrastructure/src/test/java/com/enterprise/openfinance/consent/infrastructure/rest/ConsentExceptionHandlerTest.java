package com.enterprise.openfinance.consent.infrastructure.rest;

import com.enterprise.openfinance.consent.infrastructure.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ConsentExceptionHandlerTest {

    private final ConsentExceptionHandler handler = new ConsentExceptionHandler();

    @Test
    void shouldMapIllegalArgumentExceptionToBadRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn("ix-001");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(
                new IllegalArgumentException("invalid request payload"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("invalid request payload");
        assertThat(response.getBody().interactionId()).isEqualTo("ix-001");
    }

    @Test
    void shouldMapIllegalStateExceptionToConflict() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn("ix-002");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new IllegalStateException("consent expired"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message()).isEqualTo("consent expired");
        assertThat(response.getBody().interactionId()).isEqualTo("ix-002");
    }

    @Test
    void shouldMapUnexpectedExceptionToInternalServerError() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("X-FAPI-Interaction-ID")).thenReturn("ix-003");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Unexpected error occurred");
        assertThat(response.getBody().interactionId()).isEqualTo("ix-003");
    }
}
