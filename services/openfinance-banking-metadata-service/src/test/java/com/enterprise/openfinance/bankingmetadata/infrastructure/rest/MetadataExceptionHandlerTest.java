package com.enterprise.openfinance.bankingmetadata.infrastructure.rest;

import com.enterprise.openfinance.bankingmetadata.domain.exception.ForbiddenException;
import com.enterprise.openfinance.bankingmetadata.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.bankingmetadata.infrastructure.rest.dto.MetadataErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class MetadataExceptionHandlerTest {

    private final MetadataExceptionHandler handler = new MetadataExceptionHandler();

    @Test
    void shouldMapForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-bankingmetadata");

        ResponseEntity<MetadataErrorResponse> response = handler.handleForbidden(new ForbiddenException("forbidden"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void shouldMapNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-bankingmetadata");

        ResponseEntity<MetadataErrorResponse> response = handler.handleNotFound(new ResourceNotFoundException("not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void shouldMapBadRequestAndUnexpectedErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-bankingmetadata");

        ResponseEntity<MetadataErrorResponse> badRequest = handler.handleBadRequest(new IllegalArgumentException("invalid"), request);
        ResponseEntity<MetadataErrorResponse> unexpected = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getBody()).isNotNull();
        assertThat(badRequest.getBody().code()).isEqualTo("INVALID_REQUEST");

        assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(unexpected.getBody()).isNotNull();
        assertThat(unexpected.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
