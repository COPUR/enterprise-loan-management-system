package com.enterprise.openfinance.uc15.infrastructure.rest;

import com.enterprise.openfinance.uc15.infrastructure.rest.dto.AtmErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AtmDataExceptionHandlerTest {

    private final AtmDataExceptionHandler handler = new AtmDataExceptionHandler();

    @Test
    void shouldMapInvalidRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-uc15-err-1");

        ResponseEntity<AtmErrorResponse> response = handler.handleInvalidRequest(
                new IllegalArgumentException("bad"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldMapUnexpectedError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-uc15-err-2");

        ResponseEntity<AtmErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
