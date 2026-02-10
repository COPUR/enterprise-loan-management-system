package com.enterprise.openfinance.uc12.infrastructure.rest;

import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.uc12.domain.port.in.OnboardingUseCase;
import com.enterprise.openfinance.uc12.domain.query.GetOnboardingAccountQuery;
import com.enterprise.openfinance.uc12.infrastructure.rest.dto.OnboardingAccountCreateRequest;
import com.enterprise.openfinance.uc12.infrastructure.rest.dto.OnboardingAccountResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/open-finance/v1")
public class OnboardingController {

    private final OnboardingUseCase useCase;

    public OnboardingController(OnboardingUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/accounts")
    public ResponseEntity<OnboardingAccountResponse> createAccount(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestBody @Valid OnboardingAccountCreateRequest request
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        var result = useCase.createAccount(request.toCommand(tppId, interactionId, idempotencyKey));
        String self = "/open-finance/v1/accounts/" + result.account().accountId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create(self))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-OF-Idempotency", result.idempotencyReplay() ? "HIT" : "MISS")
                .body(OnboardingAccountResponse.from(result, self));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<OnboardingAccountResponse> getAccount(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String accountId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        Optional<OnboardingAccountItemResult> result = useCase.getAccount(
                new GetOnboardingAccountQuery(accountId, tppId, interactionId)
        );
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                    .header("X-FAPI-Interaction-ID", interactionId)
                    .build();
        }

        OnboardingAccountResponse response = OnboardingAccountResponse.from(
                result.orElseThrow(),
                "/open-finance/v1/accounts/" + accountId
        );

        String etag = generateEtag(response);
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                    .header("X-FAPI-Interaction-ID", interactionId)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.orElseThrow().cacheHit() ? "HIT" : "MISS")
                .eTag(etag)
                .body(response);
    }

    private static String generateEtag(OnboardingAccountResponse response) {
        String signature = response.data().account().accountId() + '|'
                + response.data().account().status() + '|'
                + response.data().account().updatedAt();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signature.getBytes(StandardCharsets.UTF_8));
            return '"' + Base64.getUrlEncoder().withoutPadding().encodeToString(hash) + '"';
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to generate ETag", exception);
        }
    }

    private static String resolveTppId(String financialId) {
        if (financialId == null || financialId.isBlank()) {
            return "UNKNOWN_TPP";
        }
        return financialId.trim();
    }

    private static void validateSecurityHeaders(String authorization,
                                                String dpop,
                                                String interactionId) {
        boolean validAuthorization = authorization.startsWith("DPoP ") || authorization.startsWith("Bearer ");
        if (!validAuthorization) {
            throw new IllegalArgumentException("Authorization header must use Bearer or DPoP token type");
        }
        if (dpop.isBlank()) {
            throw new IllegalArgumentException("DPoP header is required");
        }
        if (interactionId.isBlank()) {
            throw new IllegalArgumentException("X-FAPI-Interaction-ID header is required");
        }
    }
}
