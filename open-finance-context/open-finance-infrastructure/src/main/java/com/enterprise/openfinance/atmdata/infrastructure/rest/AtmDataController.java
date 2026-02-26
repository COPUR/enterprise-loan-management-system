package com.enterprise.openfinance.atmdata.infrastructure.rest;

import com.enterprise.openfinance.atmdata.domain.port.in.AtmDataUseCase;
import com.enterprise.openfinance.atmdata.domain.query.GetAtmsQuery;
import com.enterprise.openfinance.atmdata.infrastructure.rest.dto.AtmResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/open-finance/v1")
public class AtmDataController {

    private static final int MAX_AGE_SECONDS = 60;

    private final AtmDataUseCase useCase;

    public AtmDataController(AtmDataUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/atms")
    public ResponseEntity<AtmResponse> listAtms(
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "lat", required = false) Double latitude,
            @RequestParam(value = "long", required = false) Double longitude,
            @RequestParam(value = "radius", required = false) Double radiusKm,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateAuthorization(authorization);

        GetAtmsQuery query = new GetAtmsQuery(interactionId, latitude, longitude, radiusKm);
        var result = useCase.listAtms(query);

        String self = buildSelfLink(latitude, longitude, radiusKm);
        AtmResponse response = AtmResponse.from(result, self);
        String etag = generateEtag(response);

        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .cacheControl(CacheControl.maxAge(MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePublic())
                    .header("X-FAPI-Interaction-ID", interactionId)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePublic())
                .header("X-FAPI-Interaction-ID", interactionId)
                .header("X-OF-Cache", result.cacheHit() ? "HIT" : "MISS")
                .eTag(etag)
                .body(response);
    }

    private static String buildSelfLink(Double latitude, Double longitude, Double radiusKm) {
        StringBuilder builder = new StringBuilder("/open-finance/v1/atms");
        if (latitude != null || longitude != null || radiusKm != null) {
            builder.append('?');
            boolean appended = false;
            if (latitude != null) {
                builder.append("lat=").append(latitude);
                appended = true;
            }
            if (longitude != null) {
                if (appended) {
                    builder.append('&');
                }
                builder.append("long=").append(longitude);
                appended = true;
            }
            if (radiusKm != null) {
                if (appended) {
                    builder.append('&');
                }
                builder.append("radius=").append(radiusKm);
            }
        }
        return builder.toString();
    }

    private static String generateEtag(AtmResponse response) {
        String signature = response.data().atm().stream()
                .map(item -> item.atmId() + '|' + item.updatedAt())
                .reduce(new StringBuilder().append(response.meta().totalRecords()).append('|'),
                        (builder, value) -> builder.append(value).append(','),
                        StringBuilder::append)
                .toString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signature.getBytes(StandardCharsets.UTF_8));
            return '"' + Base64.getUrlEncoder().withoutPadding().encodeToString(hash) + '"';
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to generate ETag", exception);
        }
    }

    private static void validateAuthorization(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return;
        }
        boolean validAuthorization = authorization.startsWith("DPoP ") || authorization.startsWith("Bearer ");
        if (!validAuthorization) {
            throw new IllegalArgumentException("Authorization header must use Bearer or DPoP token type");
        }
    }
}
