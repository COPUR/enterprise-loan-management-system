package com.bank.loanmanagement.loan.infrastructure.web.dto;

import com.bank.loan.loan.domain.customer.CustomerId;
import com.bank.loanmanagement.loan.domain.loan.*;
import com.bank.loanmanagement.loan.domain.shared.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

/**
 * Comprehensive loan recommendation request DTO
 * Maps HTTP requests to domain commands following hexagonal architecture
 */
@Schema(description = "Comprehensive loan recommendation request with customer financial profile")
public record LoanRecommendationRequestDto(
        @Schema(description = "Unique customer identifier", example = "CUST-12345")
        @NotBlank String customerId,
        
        @Schema(description = "Desired loan amount", example = "25000.00")
        @NotNull @Positive BigDecimal desiredAmount,
        
        @Schema(description = "Currency code", example = "USD")
        @NotBlank String currency,
        
        @Schema(description = "Purpose of the loan", example = "HOME_IMPROVEMENT")
        @NotBlank String loanPurpose,
        
        @Schema(description = "Comprehensive customer financial profile for AI analysis")
        @NotNull @Valid FinancialProfileDto financialProfile,
        
        @Schema(description = "Optional customer preferences for loan terms")
        @Valid PreferencesDto preferences
) {
    
    public LoanRecommendationUseCase.LoanRecommendationCommand toDomainCommand() {
        return new LoanRecommendationUseCase.LoanRecommendationCommand(
                CustomerId.of(customerId),
                Money.of(desiredAmount, currency),
                loanPurpose,
                financialProfile.toDomain(),
                preferences != null ? preferences.toDomain() : LoanTermPreferences.defaultPreferences()
        );
    }

    @Schema(description = "Customer financial profile for risk assessment")
    public record FinancialProfileDto(
            @Schema(description = "Monthly gross income", example = "6500.00")
            @NotNull @Positive BigDecimal monthlyIncome,
            
            @Schema(description = "Monthly total expenses", example = "3200.00")
            @NotNull @Positive BigDecimal monthlyExpenses,
            
            @Schema(description = "Total existing debt obligations", example = "850.00")
            @NotNull BigDecimal existingDebt,
            
            @Schema(description = "Credit score", example = "785")
            @Min(300) @Max(850) int creditScore,
            
            @Schema(description = "Employment status", example = "FULL_TIME")
            @NotBlank String employmentStatus,
            
            @Schema(description = "Employment duration in months", example = "48")
            @Min(0) int employmentDurationMonths
    ) {
        
        public CustomerFinancialProfile toDomain() {
            return new CustomerFinancialProfile(
                    Money.of(monthlyIncome, "USD"),
                    Money.of(monthlyExpenses, "USD"),
                    Money.of(existingDebt, "USD"),
                    CreditScore.of(creditScore),
                    EmploymentStatus.valueOf(employmentStatus.toUpperCase()),
                    employmentDurationMonths
            );
        }
    }

    @Schema(description = "Customer preferences for loan terms")
    public record PreferencesDto(
            @Schema(description = "Preferred loan term in months", example = "60")
            Integer preferredTermMonths,
            
            @Schema(description = "Maximum acceptable interest rate percentage", example = "8.5")
            Double maxAcceptableRatePercentage,
            
            @Schema(description = "Maximum acceptable monthly payment", example = "500.00")
            BigDecimal maxMonthlyPayment,
            
            @Schema(description = "Preference for fixed vs variable rate", example = "true")
            Boolean preferFixedRate,
            
            @Schema(description = "Desired loan features")
            List<String> desiredFeatures
    ) {
        
        public LoanTermPreferences toDomain() {
            return new LoanTermPreferences(
                    preferredTermMonths != null ? LoanTerm.ofMonths(preferredTermMonths) : null,
                    maxAcceptableRatePercentage != null ? InterestRate.ofPercentage(maxAcceptableRatePercentage) : null,
                    maxMonthlyPayment != null ? Money.of(maxMonthlyPayment, "USD") : null,
                    preferFixedRate != null ? preferFixedRate : true,
                    desiredFeatures != null ? desiredFeatures : List.of()
            );
        }
    }
}