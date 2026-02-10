package com.enterprise.openfinance.uc14.infrastructure.rest;

import com.enterprise.openfinance.uc14.infrastructure.rest.dto.ProductErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ProductDataExceptionHandlerTest {

    private final ProductDataExceptionHandler handler = new ProductDataExceptionHandler();

    @Test
    void shouldMapIllegalArgumentExceptionToBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-uc14-err-1");

        ResponseEntity<ProductErrorResponse> response = handler.handleInvalidRequest(
                new IllegalArgumentException("bad input"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldMapUnexpectedExceptionToInternalServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-uc14-err-2");

        ResponseEntity<ProductErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("boom"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
