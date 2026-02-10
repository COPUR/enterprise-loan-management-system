package com.enterprise.openfinance.uc14.infrastructure.rest;

import com.enterprise.openfinance.uc14.domain.port.in.ProductDataUseCase;
import com.enterprise.openfinance.uc14.domain.query.GetProductsQuery;
import com.enterprise.openfinance.uc14.infrastructure.rest.dto.ProductsResponse;
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
public class ProductDataController {

    private static final int MAX_AGE_SECONDS = 60;

    private final ProductDataUseCase useCase;

    public ProductDataController(ProductDataUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/products")
    public ResponseEntity<ProductsResponse> listProducts(
            @RequestHeader("X-FAPI-Interaction-ID") @NotBlank String interactionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "segment", required = false) String segment,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        validateAuthorization(authorization);

        GetProductsQuery query = new GetProductsQuery(interactionId, type, segment);
        var result = useCase.listProducts(query);

        String self = buildSelfLink(type, segment);
        ProductsResponse response = ProductsResponse.from(result, self);
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

    private static String buildSelfLink(String type, String segment) {
        StringBuilder builder = new StringBuilder("/open-finance/v1/products");
        if (type != null || segment != null) {
            builder.append('?');
            boolean appended = false;
            if (type != null && !type.isBlank()) {
                builder.append("type=").append(type.trim());
                appended = true;
            }
            if (segment != null && !segment.isBlank()) {
                if (appended) {
                    builder.append('&');
                }
                builder.append("segment=").append(segment.trim());
            }
        }
        return builder.toString();
    }

    private static String generateEtag(ProductsResponse response) {
        String signature = response.data().product().stream()
                .map(item -> item.productId() + '|' + item.updatedAt())
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
