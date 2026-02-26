package com.enterprise.openfinance.insurancedata.infrastructure.rest;

import com.enterprise.openfinance.insurancedata.domain.port.in.InsuranceDataUseCase;
import com.enterprise.openfinance.insurancedata.domain.query.GetMotorPoliciesQuery;
import com.enterprise.openfinance.insurancedata.domain.query.GetMotorPolicyQuery;
import com.enterprise.openfinance.insurancedata.infrastructure.rest.dto.InsurancePoliciesResponse;
import com.enterprise.openfinance.insurancedata.infrastructure.rest.dto.InsurancePolicyResponse;
import jakarta.validation.constraints.NotBlank;
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
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@RestController
@Validated
@RequestMapping("/open-insurance/v1")
public class InsuranceDataController {

    private final InsuranceDataUseCase useCase;

    public InsuranceDataController(InsuranceDataUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/motor-insurance-policies")
    public ResponseEntity<InsurancePoliciesResponse> listMotorPolicies(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = useCase.listMotorPolicies(new GetMotorPoliciesQuery(consentId, tppId, interactionId, page, pageSize));
        String selfLink = buildListLink(result.page(), result.pageSize());
        String nextLink = result.nextPage().map(next -> buildListLink(next, result.pageSize())).orElse(null);
        InsurancePoliciesResponse response = InsurancePoliciesResponse.from(result, selfLink, nextLink);
        String etag = generateListEtag(response);

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

    @GetMapping("/motor-insurance-policies/{policyId}")
    public ResponseEntity<InsurancePolicyResponse> getMotorPolicy(
            @RequestHeader("Authorization") @NotBlank String authorization,
            @RequestHeader("DPoP") @NotBlank String dpop,
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader("X-Consent-ID") @NotBlank String consentId,
            @RequestHeader(value = "x-fapi-financial-id", required = false) String financialId,
            @PathVariable @NotBlank String policyId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateSecurityHeaders(authorization, dpop, interactionId, consentId);
        String tppId = resolveTppId(financialId);

        var result = useCase.getMotorPolicy(new GetMotorPolicyQuery(consentId, tppId, policyId, interactionId));
        InsurancePolicyResponse response = InsurancePolicyResponse.from(result, "/open-insurance/v1/motor-insurance-policies/" + policyId);
        String etag = generatePolicyEtag(response);

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

    private static String buildListLink(int page, int pageSize) {
        return "/open-insurance/v1/motor-insurance-policies?page=" + page + "&pageSize=" + pageSize;
    }

    private static String generateListEtag(InsurancePoliciesResponse response) {
        String signature = response.data().policies().stream()
                .map(InsurancePoliciesResponse.PolicyData::policyId)
                .reduce(new StringBuilder()
                                .append(response.meta().page())
                                .append('|')
                                .append(response.meta().pageSize())
                                .append('|')
                                .append(response.meta().totalRecords())
                                .append('|'),
                        (builder, id) -> builder.append(id).append(','),
                        StringBuilder::append)
                .toString();
        return hashSignature(signature);
    }

    private static String generatePolicyEtag(InsurancePolicyResponse response) {
        String signature = response.data().policy().policyId() + '|'
                + response.data().policy().policyNumber() + '|'
                + response.data().policy().status() + '|'
                + response.data().policy().endDate();
        return hashSignature(signature);
    }

    private static String hashSignature(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
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
