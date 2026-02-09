package com.enterprise.openfinance.uc03.infrastructure.rest;

import com.enterprise.openfinance.uc03.domain.port.in.ConfirmationOfPayeeUseCase;
import com.enterprise.openfinance.uc03.infrastructure.rest.dto.ConfirmationOfPayeeRequest;
import com.enterprise.openfinance.uc03.infrastructure.rest.dto.ConfirmationOfPayeeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/open-finance/v1/confirmation-of-payee")
public class ConfirmationOfPayeeController {

    private final ConfirmationOfPayeeUseCase confirmationOfPayeeUseCase;

    public ConfirmationOfPayeeController(ConfirmationOfPayeeUseCase confirmationOfPayeeUseCase) {
        this.confirmationOfPayeeUseCase = confirmationOfPayeeUseCase;
    }

    @PostMapping("/confirmation")
    public ResponseEntity<ConfirmationOfPayeeResponse> confirm(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestBody @Valid ConfirmationOfPayeeRequest request
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId);

        String tppId = (financialId == null || financialId.isBlank()) ? "UNKNOWN_TPP" : financialId.trim();
        var result = confirmationOfPayeeUseCase.confirm(request.toDomain(tppId, interactionId));
        var response = ConfirmationOfPayeeResponse.from(result);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.fromCache() ? "HIT" : "MISS")
                .body(response);
    }

    private static void validateSecurityHeaders(String authorization, String dpop, String interactionId) {
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
