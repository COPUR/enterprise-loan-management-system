package com.bank.loanmanagement.loan.security.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoredTokenData {
    private String tokenId;
    private String clientId;
    private Long issuedAt;
    private Long expiresAt;
    private String scope;
    private TokenBinding binding;
}