package com.bank.loanmanagement.security.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FAPITokenResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String refreshToken;
    private String scope;
}