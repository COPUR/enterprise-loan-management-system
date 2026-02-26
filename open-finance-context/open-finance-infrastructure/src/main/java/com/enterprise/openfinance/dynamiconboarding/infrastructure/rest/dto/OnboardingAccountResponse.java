package com.enterprise.openfinance.dynamiconboarding.infrastructure.rest.dto;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OnboardingAccountResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links
) {

    public static OnboardingAccountResponse from(OnboardingAccountResult result, String self) {
        return new OnboardingAccountResponse(
                new Data(Account.from(result.account())),
                new Links(self)
        );
    }

    public static OnboardingAccountResponse from(OnboardingAccountItemResult result, String self) {
        return new OnboardingAccountResponse(
                new Data(Account.from(result.account())),
                new Links(self)
        );
    }

    public record Data(
            @JsonProperty("Account") Account account
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record Account(
            @JsonProperty("AccountId") String accountId,
            @JsonProperty("CustomerId") String customerId,
            @JsonProperty("Status") String status,
            @JsonProperty("PrimaryCurrency") String primaryCurrency,
            @JsonProperty("RejectionReason") String rejectionReason,
            @JsonProperty("Applicant") Applicant applicant,
            @JsonProperty("CreatedAt") String createdAt,
            @JsonProperty("UpdatedAt") String updatedAt
    ) {

        static Account from(OnboardingAccount account) {
            OnboardingApplicantProfile profile = account.applicantProfile();
            return new Account(
                    account.accountId(),
                    account.customerId(),
                    account.status().apiValue(),
                    account.primaryCurrency(),
                    account.rejectionReason(),
                    new Applicant(
                            profile.fullName(),
                            profile.nationalId(),
                            profile.countryCode()
                    ),
                    account.createdAt().toString(),
                    account.updatedAt().toString()
            );
        }
    }

    public record Applicant(
            @JsonProperty("FullName") String fullName,
            @JsonProperty("NationalId") String nationalId,
            @JsonProperty("CountryCode") String countryCode
    ) {
    }
}
