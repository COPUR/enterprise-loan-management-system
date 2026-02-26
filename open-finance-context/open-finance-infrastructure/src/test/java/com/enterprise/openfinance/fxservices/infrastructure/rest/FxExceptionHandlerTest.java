package com.enterprise.openfinance.fxservices.infrastructure.rest;

import com.enterprise.openfinance.fxservices.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.fxservices.domain.exception.ForbiddenException;
import com.enterprise.openfinance.fxservices.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.fxservices.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.fxservices.domain.exception.ServiceUnavailableException;
import com.enterprise.openfinance.fxservices.infrastructure.rest.dto.FxErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class FxExceptionHandlerTest {

    @Test
    void shouldMapDomainExceptions() {
        FxExceptionHandler handler = new FxExceptionHandler();
        MockHttpServletRequest request = request();

        ResponseEntity<FxErrorResponse> forbidden = handler.handleForbidden(new ForbiddenException("forbidden"), request);
        ResponseEntity<FxErrorResponse> notFound = handler.handleNotFound(new ResourceNotFoundException("missing"), request);
        ResponseEntity<FxErrorResponse> conflict = handler.handleConflict(new IdempotencyConflictException("conflict"), request);
        ResponseEntity<FxErrorResponse> business = handler.handleBusinessRule(new BusinessRuleViolationException("rule"), request);
        ResponseEntity<FxErrorResponse> unavailable = handler.handleUnavailable(new ServiceUnavailableException("closed"), request);
        ResponseEntity<FxErrorResponse> badRequest = handler.handleBadRequest(new IllegalArgumentException("bad"), request);
        ResponseEntity<FxErrorResponse> unknown = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(business.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-1");
        return request;
    }
}
