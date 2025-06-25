package com.bank.loanmanagement.ai.infrastructure.web.dto;

import com.bank.loanmanagement.ai.application.port.in.AnalyzeLoanRequestCommand;
import com.bank.loanmanagement.ai.domain.model.EmploymentType;
import com.bank.loanmanagement.ai.domain.model.LoanPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for AI loan analysis requests
 */
@Data
@Schema(description = "AI loan analysis request")
public class AiLoanAnalysisRequest {

    @Schema(description = "Unique request identifier", example = "REQ-123456")
    private String requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8);

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is $1,000")
    @DecimalMax(value = "10000000.00", message = "Maximum loan amount is $10,000,000")
    @Schema(description = "Requested loan amount", example = "250000.00")
    private BigDecimal requestedAmount;

    @NotBlank(message = "Applicant name is required")
    @Size(max = 100, message = "Applicant name must not exceed 100 characters")
    @Schema(description = "Full name of the loan applicant", example = "John Smith")
    private String applicantName;

    @NotBlank(message = "Applicant ID is required")
    @Size(max = 50, message = "Applicant ID must not exceed 50 characters")
    @Schema(description = "Unique identifier for the applicant", example = "CUST-12345")
    private String applicantId;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.01", message = "Monthly income must be positive")
    @Schema(description = "Applicant's monthly income", example = "8000.00")
    private BigDecimal monthlyIncome;

    @DecimalMin(value = "0.00", message = "Monthly expenses cannot be negative")
    @Schema(description = "Applicant's monthly expenses", example = "3500.00")
    private BigDecimal monthlyExpenses;

    @NotNull(message = "Employment type is required")
    @Schema(description = "Type of employment", example = "FULL_TIME")
    private EmploymentType employmentType;

    @Min(value = 0, message = "Employment tenure cannot be negative")
    @Max(value = 600, message = "Employment tenure seems unrealistic")
    @Schema(description = "Employment tenure in months", example = "24")
    private Integer employmentTenureMonths;

    @NotNull(message = "Loan purpose is required")
    @Schema(description = "Purpose of the loan", example = "HOME_PURCHASE")
    private LoanPurpose loanPurpose;

    @Min(value = 1, message = "Loan term must be at least 1 month")
    @Max(value = 360, message = "Maximum loan term is 360 months")
    @Schema(description = "Requested loan term in months", example = "360")
    private Integer requestedTermMonths;

    @DecimalMin(value = "0.00", message = "Current debt cannot be negative")
    @Schema(description = "Current total debt", example = "15000.00")
    private BigDecimal currentDebt;

    @Min(value = 300, message = "Credit score seems too low")
    @Max(value = 850, message = "Credit score seems too high")
    @Schema(description = "Credit score", example = "720")
    private Integer creditScore;

    @Size(max = 1000, message = "Natural language request must not exceed 1000 characters")
    @Schema(description = "Additional natural language description of the request")
    private String naturalLanguageRequest;

    @Schema(description = "Additional application data")
    private Map<String, Object> additionalData = new HashMap<>();

    /**
     * Convert DTO to command
     */
    public AnalyzeLoanRequestCommand toCommand() {
        return new AnalyzeLoanRequestCommand(
            requestId,
            requestedAmount,
            applicantName,
            applicantId,
            monthlyIncome,
            monthlyExpenses,
            employmentType,
            employmentTenureMonths,
            loanPurpose,
            requestedTermMonths,
            currentDebt,
            creditScore,
            naturalLanguageRequest,
            additionalData
        );
    }
}