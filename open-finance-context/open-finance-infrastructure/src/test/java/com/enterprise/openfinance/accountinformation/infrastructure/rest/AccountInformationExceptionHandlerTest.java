package com.enterprise.openfinance.accountinformation.infrastructure.rest;

import com.enterprise.openfinance.accountinformation.domain.exception.ForbiddenException;
import com.enterprise.openfinance.accountinformation.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.accountinformation.infrastructure.rest.dto.AisErrorResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AccountInformationExceptionHandlerTest {

    private final AccountInformationExceptionHandler handler = new AccountInformationExceptionHandler();

    @Test
    void shouldMapForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-account-information");

        ResponseEntity<AisErrorResponse> response = handler.handleForbidden(
                new ForbiddenException("Scope is insufficient"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void shouldMapNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-account-information");

        ResponseEntity<AisErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Account not found"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void shouldMapBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-account-information");

        ResponseEntity<AisErrorResponse> response = handler.handleBadRequest(
                new IllegalArgumentException("Invalid query"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldMapUnexpectedErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-FAPI-Interaction-ID", "ix-account-information");

        ResponseEntity<AisErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("Boom"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
