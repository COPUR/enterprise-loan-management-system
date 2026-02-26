package com.enterprise.openfinance.paymentinitiation.infrastructure.rest;

import com.enterprise.openfinance.paymentinitiation.domain.port.in.PaymentInitiationUseCase;
import com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto.PaymentRequest;
import com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto.PaymentResponse;
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
import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/open-finance/v1/payments")
public class PaymentInitiationController {

    private final PaymentInitiationUseCase paymentInitiationUseCase;

    public PaymentInitiationController(PaymentInitiationUseCase paymentInitiationUseCase) {
        this.paymentInitiationUseCase = paymentInitiationUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> submitPayment(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader("x-jws-signature") @NotBlank String jwsSignature,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestBody @Valid PaymentRequest request
    ) {
        validatePostSecurityHeaders(authorization, dpop, interactionId, idempotencyKey, jwsSignature);
        String tppId = (financialId == null || financialId.isBlank()) ? "UNKNOWN_TPP" : financialId.trim();

        var result = paymentInitiationUseCase.submitPayment(
                request.toCommand(tppId, idempotencyKey, interactionId, jwsSignature, request.rawCanonicalPayload())
        );
        PaymentResponse response = PaymentResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/open-finance/v1/payments/" + result.paymentId()))
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-OF-Idempotency", result.idempotencyReplay() ? "HIT" : "MISS")
                .body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @PathVariable @NotBlank String paymentId
    ) {
        validateGetSecurityHeaders(authorization, dpop, interactionId);
        return paymentInitiationUseCase.getPayment(paymentId)
                .map(PaymentResponse::from)
                .map(response -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                        .header("X-FAPI-Interaction-ID", interactionId)
                        .body(response))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static void validatePostSecurityHeaders(
            String authorization,
            String dpop,
            String interactionId,
            String idempotencyKey,
            String jwsSignature
    ) {
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
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("X-Idempotency-Key header is required");
        }
        if (jwsSignature.isBlank()) {
            throw new IllegalArgumentException("x-jws-signature header is required");
        }
    }

    private static void validateGetSecurityHeaders(
            String authorization,
            String dpop,
            String interactionId
    ) {
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
