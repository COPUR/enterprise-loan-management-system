package com.enterprise.openfinance.uc09.infrastructure.rest.dto;

import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyItemResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InsurancePolicyResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static InsurancePolicyResponse from(InsurancePolicyItemResult result, String self) {
        return new InsurancePolicyResponse(
                new Data(InsurancePoliciesResponse.PolicyData.from(result.policy())),
                new Links(self)
        );
    }

    public record Data(
            @JsonProperty("Policy") InsurancePoliciesResponse.PolicyData policy
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }
}
