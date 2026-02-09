package com.enterprise.openfinance.uc04.infrastructure.rest;

import com.enterprise.openfinance.uc04.domain.port.in.MetadataUseCase;
import com.enterprise.openfinance.uc04.domain.query.GetAccountMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetPartiesMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetStandingOrdersMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetTransactionMetadataQuery;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.AccountMetadataResponse;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.PartiesMetadataResponse;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.StandingOrdersMetadataResponse;
import com.enterprise.openfinance.uc04.infrastructure.rest.dto.TransactionMetadataResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/open-finance/v1/metadata")
public class MetadataController {

    private final MetadataUseCase metadataUseCase;

    public MetadataController(MetadataUseCase metadataUseCase) {
        this.metadataUseCase = metadataUseCase;
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<TransactionMetadataResponse> getTransactionMetadata(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String accountId,
            @RequestParam(value = "fromBookingDateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromBookingDateTime,
            @RequestParam(value = "toBookingDateTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toBookingDateTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = metadataUseCase.getTransactions(new GetTransactionMetadataQuery(
                consentId,
                tppId,
                accountId,
                interactionId,
                fromBookingDateTime,
                toBookingDateTime,
                page,
                pageSize
        ));

        String selfLink = buildTransactionLink(accountId, fromBookingDateTime, toBookingDateTime, result.page(), result.pageSize());
        String nextLink = result.nextPage()
                .map(nextPage -> buildTransactionLink(accountId, fromBookingDateTime, toBookingDateTime, nextPage, result.pageSize()))
                .orElse(null);
        TransactionMetadataResponse response = TransactionMetadataResponse.from(result, selfLink, nextLink);
        String etag = generateTransactionEtag(response);

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
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .eTag(etag)
                .body(response);
    }

    @GetMapping("/accounts/{accountId}/parties")
    public ResponseEntity<PartiesMetadataResponse> getPartiesMetadata(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String accountId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = metadataUseCase.getParties(new GetPartiesMetadataQuery(consentId, tppId, accountId, interactionId));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .body(PartiesMetadataResponse.from(accountId, result));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountMetadataResponse> getAccountMetadata(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String accountId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = metadataUseCase.getAccountMetadata(new GetAccountMetadataQuery(consentId, tppId, accountId, interactionId));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .body(AccountMetadataResponse.from(result.item()));
    }

    @GetMapping("/standing-orders")
    public ResponseEntity<StandingOrdersMetadataResponse> getStandingOrdersMetadata(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestParam(value = "accountId", required = false) String accountId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = metadataUseCase.getStandingOrders(new GetStandingOrdersMetadataQuery(
                consentId,
                tppId,
                accountId,
                interactionId,
                page,
                pageSize
        ));

        String selfLink = buildStandingOrdersLink(accountId, result.page(), result.pageSize());
        String nextLink = result.nextPage()
                .map(nextPage -> buildStandingOrdersLink(accountId, nextPage, result.pageSize()))
                .orElse(null);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .body(StandingOrdersMetadataResponse.from(result, selfLink, nextLink));
    }

    private static String buildTransactionLink(String accountId,
                                               Instant fromBookingDateTime,
                                               Instant toBookingDateTime,
                                               int page,
                                               int pageSize) {
        StringBuilder builder = new StringBuilder("/open-finance/v1/metadata/accounts/")
                .append(accountId)
                .append("/transactions?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        if (fromBookingDateTime != null) {
            builder.append("&fromBookingDateTime=").append(fromBookingDateTime);
        }
        if (toBookingDateTime != null) {
            builder.append("&toBookingDateTime=").append(toBookingDateTime);
        }
        return builder.toString();
    }

    private static String buildStandingOrdersLink(String accountId, int page, int pageSize) {
        StringBuilder builder = new StringBuilder("/open-finance/v1/metadata/standing-orders?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);
        if (accountId != null && !accountId.isBlank()) {
            builder.append("&accountId=").append(accountId);
        }
        return builder.toString();
    }

    private static String generateTransactionEtag(TransactionMetadataResponse response) {
        String signature = response.data().transactions().stream()
                .map(TransactionMetadataResponse.TransactionData::transactionId)
                .reduce(new StringBuilder()
                                .append(response.meta().page())
                                .append('|')
                                .append(response.meta().pageSize())
                                .append('|')
                                .append(response.meta().totalRecords())
                                .append('|'),
                        (builder, id) -> builder.append(id).append(','),
                        (left, right) -> left.append(right))
                .toString();

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
                                                String interactionId,
                                                String consentId) {
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
        if (consentId.isBlank()) {
            throw new IllegalArgumentException("X-Consent-ID header is required");
        }
    }
}
