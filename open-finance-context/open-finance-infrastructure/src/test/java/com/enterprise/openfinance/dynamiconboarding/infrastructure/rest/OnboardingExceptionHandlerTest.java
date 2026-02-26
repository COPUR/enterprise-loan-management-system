package com.enterprise.openfinance.dynamiconboarding.infrastructure.rest;

import com.enterprise.openfinance.dynamiconboarding.domain.exception.ComplianceViolationException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.DecryptionFailedException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.ForbiddenException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.IdempotencyConflictException;
import com.enterprise.openfinance.dynamiconboarding.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.dynamiconboarding.infrastructure.rest.dto.OnboardingErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class OnboardingExceptionHandlerTest {

    @Test
    void shouldMapDomainExceptions() {
        OnboardingExceptionHandler handler = new OnboardingExceptionHandler();
        MockHttpServletRequest request = request();

        ResponseEntity<OnboardingErrorResponse> forbidden = handler.handleForbidden(new ForbiddenException("forbidden"), request);
        ResponseEntity<OnboardingErrorResponse> notFound = handler.handleNotFound(new ResourceNotFoundException("missing"), request);
        ResponseEntity<OnboardingErrorResponse> conflict = handler.handleConflict(new IdempotencyConflictException("conflict"), request);
        ResponseEntity<OnboardingErrorResponse> decryption = handler.handleDecryptionFailure(new DecryptionFailedException("decrypt"), request);
        ResponseEntity<OnboardingErrorResponse> compliance = handler.handleCompliance(new ComplianceViolationException("rejected"), request);
        ResponseEntity<OnboardingErrorResponse> badRequest = handler.handleBadRequest(new IllegalArgumentException("bad"), request);
        ResponseEntity<OnboardingErrorResponse> unknown = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(decryption.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(compliance.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-dynamic-onboarding-1");
        return request;
    }
}
