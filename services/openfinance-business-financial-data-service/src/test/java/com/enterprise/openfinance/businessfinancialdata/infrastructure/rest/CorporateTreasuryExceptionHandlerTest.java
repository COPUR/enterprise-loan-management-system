package com.enterprise.openfinance.businessfinancialdata.infrastructure.rest;

import com.enterprise.openfinance.businessfinancialdata.domain.exception.ForbiddenException;
import com.enterprise.openfinance.businessfinancialdata.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.rest.dto.CorporateErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class CorporateTreasuryExceptionHandlerTest {

    private final CorporateTreasuryExceptionHandler handler = new CorporateTreasuryExceptionHandler();

    @Test
    void shouldMapForbiddenAndNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-businessfinancialdata");

        ResponseEntity<CorporateErrorResponse> forbidden = handler.handleForbidden(new ForbiddenException("forbidden"), request);
        ResponseEntity<CorporateErrorResponse> notFound = handler.handleNotFound(new ResourceNotFoundException("not found"), request);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldMapBadRequestAndUnexpected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-businessfinancialdata");

        ResponseEntity<CorporateErrorResponse> badRequest = handler.handleBadRequest(new IllegalArgumentException("bad"), request);
        ResponseEntity<CorporateErrorResponse> unexpected = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
