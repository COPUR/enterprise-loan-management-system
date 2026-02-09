package com.enterprise.openfinance.uc02.infrastructure.rest;

import com.enterprise.openfinance.uc02.domain.port.in.AccountInformationUseCase;
import com.enterprise.openfinance.uc02.domain.query.GetAccountQuery;
import com.enterprise.openfinance.uc02.domain.query.GetBalancesQuery;
import com.enterprise.openfinance.uc02.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.uc02.domain.query.ListAccountsQuery;
import com.enterprise.openfinance.uc02.infrastructure.rest.dto.AccountResponse;
import com.enterprise.openfinance.uc02.infrastructure.rest.dto.AccountsResponse;
import com.enterprise.openfinance.uc02.infrastructure.rest.dto.BalancesResponse;
import com.enterprise.openfinance.uc02.infrastructure.rest.dto.TransactionsResponse;
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
@RequestMapping("/open-finance/v1")
public class AccountInformationController {

    private final AccountInformationUseCase accountInformationUseCase;

    public AccountInformationController(AccountInformationUseCase accountInformationUseCase) {
        this.accountInformationUseCase = accountInformationUseCase;
    }

    @GetMapping("/accounts")
    public ResponseEntity<AccountsResponse> getAccounts(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = accountInformationUseCase.listAccounts(new ListAccountsQuery(consentId, tppId, interactionId));
        AccountsResponse response = AccountsResponse.from(result.accounts(), "/open-finance/v1/accounts");

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .body(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String accountId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var account = accountInformationUseCase.getAccount(new GetAccountQuery(consentId, tppId, accountId, interactionId));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .body(AccountResponse.from(account));
    }

    @GetMapping("/accounts/{accountId}/balances")
    public ResponseEntity<BalancesResponse> getBalances(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String accountId
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = accountInformationUseCase.getBalances(new GetBalancesQuery(consentId, tppId, accountId, interactionId));
        BalancesResponse response = BalancesResponse.from(accountId, result.balances());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).noStore())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .body(response);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<TransactionsResponse> getTransactions(
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

        GetTransactionsQuery query = new GetTransactionsQuery(
                consentId,
                tppId,
                accountId,
                interactionId,
                fromBookingDateTime,
                toBookingDateTime,
                page,
                pageSize
        );

        var paged = accountInformationUseCase.getTransactions(query);
        String selfLink = buildTransactionsLink(accountId, fromBookingDateTime, toBookingDateTime, paged.page(), paged.pageSize());
        String nextLink = paged.nextPage()
                .map(nextPage -> buildTransactionsLink(accountId, fromBookingDateTime, toBookingDateTime, nextPage, paged.pageSize()))
                .orElse(null);

        TransactionsResponse response = TransactionsResponse.from(paged, selfLink, nextLink);
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
                .eTag(etag)
                .body(response);
    }

    private static String buildTransactionsLink(
            String accountId,
            Instant fromBookingDateTime,
            Instant toBookingDateTime,
            int page,
            int pageSize
    ) {
        StringBuilder builder = new StringBuilder("/open-finance/v1/accounts/")
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

    private static String resolveTppId(String financialId) {
        if (financialId == null || financialId.isBlank()) {
            return "UNKNOWN_TPP";
        }
        return financialId.trim();
    }

    private static String generateEtag(TransactionsResponse response) {
        String signature = response.data().transactions().stream()
                .map(TransactionsResponse.TransactionData::transactionId)
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
