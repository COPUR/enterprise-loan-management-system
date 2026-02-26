package com.enterprise.openfinance.insurancequotes.infrastructure.rest;

import com.enterprise.openfinance.insurancequotes.domain.exception.BusinessRuleViolationException;
import com.enterprise.openfinance.insurancequotes.domain.exception.ForbiddenException;
import com.enterprise.openfinance.insurancequotes.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.insurancequotes.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.insurancequotes.infrastructure.rest.dto.InsuranceQuoteErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InsuranceQuoteExceptionHandlerTest {

    @Test
    void shouldMapDomainAndBadRequestExceptions() {
        InsuranceQuoteExceptionHandler handler = new InsuranceQuoteExceptionHandler();
        MockHttpServletRequest request = request();

        ResponseEntity<InsuranceQuoteErrorResponse> forbidden = handler.handleForbidden(new ForbiddenException("forbidden"), request);
        ResponseEntity<InsuranceQuoteErrorResponse> notFound = handler.handleNotFound(new ResourceNotFoundException("missing"), request);
        ResponseEntity<InsuranceQuoteErrorResponse> conflict = handler.handleConflict(new IdempotencyConflictException("conflict"), request);
        ResponseEntity<InsuranceQuoteErrorResponse> businessRule = handler.handleBusinessRule(new BusinessRuleViolationException("rule"), request);
        ResponseEntity<InsuranceQuoteErrorResponse> badRequest = handler.handleBadRequest(new IllegalArgumentException("bad"), request);
        ResponseEntity<InsuranceQuoteErrorResponse> unexpected = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(businessRule.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-1");
        return request;
    }
}
