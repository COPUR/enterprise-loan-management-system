package com.enterprise.openfinance.uc06.infrastructure.rest.dto;

import com.enterprise.openfinance.uc06.domain.command.SubmitPaymentCommand;
import com.enterprise.openfinance.uc06.domain.model.PaymentInitiation;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @JsonProperty("Data")
        @NotNull @Valid Data data
) {
    public SubmitPaymentCommand toCommand(
            String tppId,
            String idempotencyKey,
            String interactionId,
            String signature,
            String rawPayload
    ) {
        return new SubmitPaymentCommand(
                tppId,
                idempotencyKey,
                data.consentId(),
                data.initiation().toDomain(),
                interactionId,
                rawPayload,
                signature
        );
    }

    public String rawCanonicalPayload() {
        Initiation initiation = data.initiation();
        return String.format(
                "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                data.consentId(),
                initiation.instructionIdentification(),
                initiation.endToEndIdentification(),
                initiation.instructedAmount().amount(),
                initiation.instructedAmount().currency(),
                initiation.creditorAccount().schemeName(),
                initiation.creditorAccount().identification(),
                initiation.debtorAccountId(),
                initiation.creditorName(),
                initiation.requestedExecutionDate()
        );
    }

    public record Data(
            @JsonProperty("ConsentId")
            @NotBlank String consentId,
            @JsonProperty("Initiation")
            @NotNull @Valid Initiation initiation
    ) {
    }

    public record Initiation(
            @JsonProperty("InstructionIdentification")
            @NotBlank String instructionIdentification,
            @JsonProperty("EndToEndIdentification")
            @NotBlank String endToEndIdentification,
            @JsonProperty("InstructedAmount")
            @NotNull @Valid Amount instructedAmount,
            @JsonProperty("CreditorAccount")
            @NotNull @Valid Account creditorAccount,
            @JsonProperty("DebtorAccountId")
            String debtorAccountId,
            @JsonProperty("CreditorName")
            String creditorName,
            @JsonProperty("RequestedExecutionDate")
            LocalDate requestedExecutionDate
    ) {
        public PaymentInitiation toDomain() {
            String debtorId = debtorAccountId == null || debtorAccountId.isBlank()
                    ? "ACC-DEBTOR-001"
                    : debtorAccountId;
            String resolvedCreditorName = creditorName;
            if (resolvedCreditorName == null || resolvedCreditorName.isBlank()) {
                resolvedCreditorName = creditorAccount.name();
            }
            if (resolvedCreditorName == null || resolvedCreditorName.isBlank()) {
                resolvedCreditorName = "UNKNOWN_CREDITOR";
            }

            return new PaymentInitiation(
                    instructionIdentification,
                    endToEndIdentification,
                    debtorId,
                    instructedAmount.asBigDecimal(),
                    instructedAmount.currency(),
                    creditorAccount.schemeName(),
                    creditorAccount.identification(),
                    resolvedCreditorName,
                    requestedExecutionDate
            );
        }
    }

    public record Amount(
            @JsonProperty("Amount")
            @NotBlank String amount,
            @JsonProperty("Currency")
            @NotBlank String currency
    ) {
        public BigDecimal asBigDecimal() {
            try {
                return new BigDecimal(amount);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("InstructedAmount.Amount must be a valid decimal");
            }
        }
    }

    public record Account(
            @JsonProperty("SchemeName")
            @NotBlank String schemeName,
            @JsonProperty("Identification")
            @NotBlank String identification,
            @JsonProperty("Name")
            String name
    ) {
    }
}
