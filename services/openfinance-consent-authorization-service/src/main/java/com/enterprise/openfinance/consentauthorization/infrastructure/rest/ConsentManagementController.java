package com.enterprise.openfinance.consentauthorization.infrastructure.rest;

import com.enterprise.openfinance.consentauthorization.domain.command.CreateConsentCommand;
import com.enterprise.openfinance.consentauthorization.domain.port.in.ConsentManagementUseCase;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.ConsentResponse;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.CreateConsentRequest;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto.RevokeConsentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@Validated
@RequestMapping("/open-finance/v1/consents")
public class ConsentManagementController {

    private final ConsentManagementUseCase consentManagementUseCase;
    private final Clock clock;

    public ConsentManagementController(ConsentManagementUseCase consentManagementUseCase, Clock clock) {
        this.consentManagementUseCase = consentManagementUseCase;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<ConsentResponse> createConsent(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestBody @Valid CreateConsentRequest request) {
        validateSecurityHeaders(authorization, dpop, interactionId);

        CreateConsentCommand command = new CreateConsentCommand(
                request.customerId(),
                request.participantId(),
                normalizeScopes(request.scopes()),
                request.purpose(),
                request.expiresAt()
        );

        var consent = consentManagementUseCase.createConsent(command);
        var response = ConsentResponse.from(consent, now());
        return ResponseEntity.created(URI.create("/open-finance/v1/consents/" + response.consentId()))
                .body(response);
    }

    @GetMapping("/{consentId}")
    public ResponseEntity<ConsentResponse> getConsent(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @PathVariable @NotBlank String consentId) {
        validateSecurityHeaders(authorization, dpop, interactionId);

        return consentManagementUseCase.getConsent(consentId)
                .map(consent -> ResponseEntity.ok(ConsentResponse.from(consent, now())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ConsentResponse>> listConsentsByCustomer(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestParam("customerId") @NotBlank String customerId) {
        validateSecurityHeaders(authorization, dpop, interactionId);

        List<ConsentResponse> responses = consentManagementUseCase.listConsentsByCustomer(customerId).stream()
                .map(consent -> ConsentResponse.from(consent, now()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{consentId}/authorize")
    public ResponseEntity<ConsentResponse> authorizeConsent(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @PathVariable @NotBlank String consentId) {
        validateSecurityHeaders(authorization, dpop, interactionId);

        Optional<ConsentResponse> result = consentManagementUseCase.authorizeConsent(consentId)
                .map(consent -> ConsentResponse.from(consent, now()));
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/{consentId}/revoke")
    public ResponseEntity<ConsentResponse> revokeConsent(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @PathVariable @NotBlank String consentId,
            @RequestBody @Valid RevokeConsentRequest request) {
        validateSecurityHeaders(authorization, dpop, interactionId);

        Optional<ConsentResponse> result = consentManagementUseCase.revokeConsent(consentId, request.reason())
                .map(consent -> ConsentResponse.from(consent, now()));
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private void validateSecurityHeaders(String authorization, String dpop, String interactionId) {
        if (!authorization.startsWith("DPoP ")) {
            throw new IllegalArgumentException("Authorization header must use DPoP token type");
        }
        if (dpop.isBlank()) {
            throw new IllegalArgumentException("DPoP header is required");
        }
        if (interactionId.isBlank()) {
            throw new IllegalArgumentException("X-FAPI-Interaction-ID header is required");
        }
    }

    private static Set<String> normalizeScopes(Set<String> scopes) {
        return scopes.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
