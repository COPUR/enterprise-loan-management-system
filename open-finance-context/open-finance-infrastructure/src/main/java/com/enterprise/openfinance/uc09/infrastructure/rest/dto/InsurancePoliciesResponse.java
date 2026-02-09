package com.enterprise.openfinance.uc09.infrastructure.rest.dto;

import com.enterprise.openfinance.uc09.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.uc09.domain.model.MotorPolicy;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InsurancePoliciesResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static InsurancePoliciesResponse from(InsurancePolicyListResult result,
                                                 String self,
                                                 String next) {
        return new InsurancePoliciesResponse(
                new Data(result.policies().stream().map(PolicyData::from).toList()),
                new Links(self, next),
                new Meta(result.page(), result.pageSize(), result.totalRecords())
        );
    }

    public record Data(
            @JsonProperty("Policies") List<PolicyData> policies
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self,
            @JsonProperty("Next") String next
    ) {
    }

    public record Meta(
            @JsonProperty("Page") int page,
            @JsonProperty("PageSize") int pageSize,
            @JsonProperty("TotalRecords") int totalRecords
    ) {
    }

    public record PolicyData(
            @JsonProperty("PolicyId") String policyId,
            @JsonProperty("PolicyNumber") String policyNumber,
            @JsonProperty("HolderNameMasked") String holderNameMasked,
            @JsonProperty("Vehicle") Vehicle vehicle,
            @JsonProperty("Premium") Premium premium,
            @JsonProperty("Coverages") List<String> coverages,
            @JsonProperty("StartDate") String startDate,
            @JsonProperty("EndDate") String endDate,
            @JsonProperty("Status") String status
    ) {

        static PolicyData from(MotorPolicy policy) {
            return new PolicyData(
                    policy.policyId(),
                    policy.policyNumber(),
                    policy.holderNameMasked(),
                    new Vehicle(policy.vehicleMake(), policy.vehicleModel(), policy.vehicleYear()),
                    new Premium(policy.premiumAmount().toPlainString(), policy.currency()),
                    policy.coverages(),
                    policy.startDate().toString(),
                    policy.endDate().toString(),
                    policy.status().apiValue()
            );
        }
    }

    public record Vehicle(
            @JsonProperty("Make") String make,
            @JsonProperty("Model") String model,
            @JsonProperty("Year") int year
    ) {
    }

    public record Premium(
            @JsonProperty("Amount") String amount,
            @JsonProperty("Currency") String currency
    ) {
    }
}
