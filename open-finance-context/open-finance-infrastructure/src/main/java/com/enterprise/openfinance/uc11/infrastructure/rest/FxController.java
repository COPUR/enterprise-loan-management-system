package com.enterprise.openfinance.uc11.infrastructure.rest;

import com.enterprise.openfinance.uc11.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.uc11.domain.port.in.FxUseCase;
import com.enterprise.openfinance.uc11.domain.query.GetFxQuoteQuery;
import com.enterprise.openfinance.uc11.infrastructure.rest.dto.FxDealRequest;
import com.enterprise.openfinance.uc11.infrastructure.rest.dto.FxDealResponse;
import com.enterprise.openfinance.uc11.infrastructure.rest.dto.FxQuoteRequest;
import com.enterprise.openfinance.uc11.infrastructure.rest.dto.FxQuoteResponse;
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
public class FxController {

    private final FxUseCase useCase;

    public FxController(FxUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/fx-quotes")
    public ResponseEntity<FxQuoteResponse> createQuote(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestBody @Valid FxQuoteRequest request
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        var result = useCase.createQuote(request.toCommand(tppId, interactionId));
        String self = "/open-finance/v1/fx-quotes/" + result.quote().quoteId();

        return ResponseEntity.ok()
                .location(URI.create(self))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Idempotency", "MISS")
                .body(FxQuoteResponse.from(result, self));
    }

    @PostMapping("/fx-deals")
    public ResponseEntity<FxDealResponse> executeDeal(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestBody @Valid FxDealRequest request
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        var result = useCase.executeDeal(request.toCommand(tppId, idempotencyKey, interactionId));
        String self = "/open-finance/v1/fx-deals/" + result.deal().dealId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create(self))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-OF-Idempotency", result.idempotencyReplay() ? "HIT" : "MISS")
                .body(FxDealResponse.from(result, self));
    }

    @GetMapping("/fx-quotes/{quoteId}")
    public ResponseEntity<FxQuoteResponse> getQuote(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String quoteId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);
        String tppId = resolveTppId(financialId);

        Optional<FxQuoteItemResult> result = useCase.getQuote(
                new GetFxQuoteQuery(quoteId, tppId, interactionId)
        );
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                    .header("X-FAPI-Interaction-ID", interactionId)
                    .build();
        }

        FxQuoteResponse response = FxQuoteResponse.from(
                result.orElseThrow(),
                "/open-finance/v1/fx-quotes/" + quoteId
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

    private static String generateEtag(FxQuoteResponse response) {
        String signature = response.data().quote().quoteId() + '|'
                + response.data().quote().status() + '|'
                + response.data().quote().validUntil() + '|'
                + response.data().quote().exchangeRate();
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
