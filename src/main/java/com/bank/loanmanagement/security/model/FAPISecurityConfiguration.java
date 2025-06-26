package com.bank.loanmanagement.security.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fapi.security")
@Data
public class FAPISecurityConfiguration {
    private String issuerUri;
    private String jwksUri;
    private Long accessTokenTtl = 3600L;
    private Long refreshTokenTtl = 86400L;
    private String signingAlgorithm = "PS256";
}