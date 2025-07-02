package com.bank.loanmanagement.loan.security.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenBinding {
    private String bindingType;
    private String bindingValue;
    private String algorithm;
}