package com.enterprise.openfinance.uc09.infrastructure.rest;

import com.enterprise.openfinance.uc09.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc09.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc09.infrastructure.rest.dto.InsuranceErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InsuranceDataExceptionHandlerTest {

    @Test
    void shouldMapDomainAndBadRequestExceptions() {
        InsuranceDataExceptionHandler handler = new InsuranceDataExceptionHandler();
        MockHttpServletRequest request = request();

        ResponseEntity<InsuranceErrorResponse> forbidden = handler.handleForbidden(new ForbiddenException("forbidden"), request);
        ResponseEntity<InsuranceErrorResponse> notFound = handler.handleNotFound(new ResourceNotFoundException("missing"), request);
        ResponseEntity<InsuranceErrorResponse> badRequest = handler.handleBadRequest(new IllegalArgumentException("bad"), request);
        ResponseEntity<InsuranceErrorResponse> unexpected = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-1");
        return request;
    }
}
