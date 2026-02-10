package com.enterprise.openfinance.businessfinancialdata.infrastructure.rest;

import com.enterprise.openfinance.businessfinancialdata.domain.port.in.CorporateTreasuryUseCase;
import com.enterprise.openfinance.businessfinancialdata.domain.query.GetCorporateBalancesQuery;
import com.enterprise.openfinance.businessfinancialdata.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.businessfinancialdata.domain.query.ListCorporateAccountsQuery;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.rest.dto.CorporateAccountsResponse;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.rest.dto.CorporateBalancesResponse;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.rest.dto.CorporateTransactionsResponse;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Validated
@RequestMapping("/open-finance/v1/corporate")
public class CorporateTreasuryController {

    private final CorporateTreasuryUseCase useCase;
    private final Map<String, String> transactionEtagCache = new ConcurrentHashMap<>();

    public CorporateTreasuryController(CorporateTreasuryUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/accounts")
    public ResponseEntity<CorporateAccountsResponse> getAccounts(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestParam(value = "includeVirtual", required = false) Boolean includeVirtual,
            @RequestParam(value = "masterAccountId", required = false) String masterAccountId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = useCase.listAccounts(new ListCorporateAccountsQuery(
                consentId,
                tppId,
                interactionId,
                includeVirtual,
                masterAccountId
        ));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .body(CorporateAccountsResponse.from(result, "/open-finance/v1/corporate/accounts"));
    }

    @GetMapping("/accounts/{masterAccountId}/balances")
    public ResponseEntity<CorporateBalancesResponse> getBalances(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String masterAccountId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = useCase.getBalances(new GetCorporateBalancesQuery(
                consentId,
                tppId,
                masterAccountId,
                interactionId
        ));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .header("X-OF-Entitlement", result.masked() ? "RESTRICTED" : "FULL")
                .body(CorporateBalancesResponse.from(masterAccountId, result));
    }

    @GetMapping("/transactions")
    public ResponseEntity<CorporateTransactionsResponse> getTransactions(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestParam(value = "accountId", required = false) String accountId,
            @RequestParam(value = "fromBookingDateTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromBookingDateTime,
            @RequestParam(value = "toBookingDateTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toBookingDateTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);
        String requestSignature = buildTransactionRequestSignature(consentId, tppId, accountId, fromBookingDateTime, toBookingDateTime, page, pageSize);

        if (ifNoneMatch != null) {
            String cachedEtag = transactionEtagCache.get(requestSignature);
            if (ifNoneMatch.equals(cachedEtag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                        .header("X-FAPI-Interaction-ID", interactionId)
                        .eTag(cachedEtag)
                        .build();
            }
        }

        var result = useCase.getTransactions(new GetCorporateTransactionsQuery(
                consentId,
                tppId,
                interactionId,
                accountId,
                fromBookingDateTime,
                toBookingDateTime,
                page,
                pageSize
        ));

        String selfLink = buildTransactionsLink(accountId, fromBookingDateTime, toBookingDateTime, result.page(), result.pageSize());
        String nextLink = result.nextPage()
                .map(nextPage -> buildTransactionsLink(accountId, fromBookingDateTime, toBookingDateTime, nextPage, result.pageSize()))
                .orElse(null);

        CorporateTransactionsResponse response = CorporateTransactionsResponse.from(result, selfLink, nextLink);
        String etag = generateEtag(response);
        transactionEtagCache.put(requestSignature, etag);

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

    private static String buildTransactionsLink(String accountId,
                                                Instant fromBookingDateTime,
                                                Instant toBookingDateTime,
                                                int page,
                                                int pageSize) {
        StringBuilder builder = new StringBuilder("/open-finance/v1/corporate/transactions?page=")
                .append(page)
                .append("&pageSize=")
                .append(pageSize);

        if (accountId != null && !accountId.isBlank()) {
            builder.append("&accountId=").append(accountId);
        }
        if (fromBookingDateTime != null) {
            builder.append("&fromBookingDateTime=").append(fromBookingDateTime);
        }
        if (toBookingDateTime != null) {
            builder.append("&toBookingDateTime=").append(toBookingDateTime);
        }
        return builder.toString();
    }

    private static String buildTransactionRequestSignature(String consentId,
                                                           String tppId,
                                                           String accountId,
                                                           Instant fromBookingDateTime,
                                                           Instant toBookingDateTime,
                                                           Integer page,
                                                           Integer pageSize) {
        return consentId + '|' + tppId + '|' + accountId + '|' + fromBookingDateTime + '|' + toBookingDateTime + '|' + page + '|' + pageSize;
    }

    private static String generateEtag(CorporateTransactionsResponse response) {
        String signature = response.data().transactions().stream()
                .map(CorporateTransactionsResponse.TransactionData::transactionId)
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
