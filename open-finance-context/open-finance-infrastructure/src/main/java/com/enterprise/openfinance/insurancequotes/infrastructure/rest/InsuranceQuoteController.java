package com.enterprise.openfinance.insurancequotes.infrastructure.rest;

import com.enterprise.openfinance.insurancequotes.domain.port.in.InsuranceQuoteUseCase;
import com.enterprise.openfinance.insurancequotes.domain.query.GetMotorQuoteQuery;
import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.insurancequotes.infrastructure.rest.dto.MotorQuoteActionRequest;
import com.enterprise.openfinance.insurancequotes.infrastructure.rest.dto.MotorQuoteRequest;
import com.enterprise.openfinance.insurancequotes.infrastructure.rest.dto.MotorQuoteResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/open-insurance/v1/motor-insurance-quotes")
public class InsuranceQuoteController {

    private final InsuranceQuoteUseCase useCase;

    public InsuranceQuoteController(InsuranceQuoteUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<MotorQuoteResponse> createQuote(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestBody @Valid MotorQuoteRequest request
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        var result = useCase.createQuote(request.toCommand(tppId, interactionId));
        String self = "/open-insurance/v1/motor-insurance-quotes/" + result.quote().quoteId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create(self))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Idempotency", "MISS")
                .body(MotorQuoteResponse.from(result, self));
    }

    @PatchMapping("/{quoteId}")
    public ResponseEntity<MotorQuoteResponse> acceptQuote(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String quoteId,
            @RequestBody @Valid MotorQuoteActionRequest request
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        var result = useCase.acceptQuote(request.toCommand(tppId, quoteId, idempotencyKey, interactionId));
        String self = "/open-insurance/v1/motor-insurance-quotes/" + result.quote().quoteId();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-OF-Idempotency", result.idempotencyReplay() ? "HIT" : "MISS")
                .body(MotorQuoteResponse.from(result, self));
    }

    @GetMapping("/{quoteId}")
    public ResponseEntity<MotorQuoteResponse> getQuote(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String quoteId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        Optional<MotorQuoteItemResult> result = useCase.getQuote(
                new GetMotorQuoteQuery(quoteId, tppId, interactionId)
        );
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                    .header("X-FAPI-Interaction-ID", interactionId)
                    .build();
        }

        MotorQuoteResponse response = MotorQuoteResponse.from(
                result.orElseThrow(),
                "/open-insurance/v1/motor-insurance-quotes/" + quoteId
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

    private static String generateEtag(MotorQuoteResponse response) {
        String signature = response.data().quote().quoteId() + '|'
                + response.data().quote().status() + '|'
                + response.data().quote().validUntil() + '|'
                + response.data().quote().policyId();
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
