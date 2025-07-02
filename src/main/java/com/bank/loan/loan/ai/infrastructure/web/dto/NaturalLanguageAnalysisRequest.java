package com.bank.loan.loan.ai.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for natural language loan analysis requests
 */
@Data
@Schema(description = "Natural language loan analysis request")
public class NaturalLanguageAnalysisRequest {

    @Schema(description = "Unique request identifier", example = "NLP-REQ-123456")
    private String requestId = "NLP-REQ-" + UUID.randomUUID().toString().substring(0, 8);

    @NotBlank(message = "Natural language text is required")
    @Size(min = 10, max = 2000, message = "Request text must be between 10 and 2000 characters")
    @Schema(description = "Natural language description of the loan request", 
            example = "I would like to apply for a $250,000 home loan. I make $8,000 per month and have been employed full-time for 3 years. My credit score is around 720.")
    private String naturalLanguageText;

    @NotBlank(message = "Applicant ID is required")
    @Size(max = 50, message = "Applicant ID must not exceed 50 characters")
    @Schema(description = "Unique identifier for the applicant", example = "CUST-12345")
    private String applicantId;

    @NotBlank(message = "Applicant name is required")
    @Size(max = 100, message = "Applicant name must not exceed 100 characters")
    @Schema(description = "Full name of the loan applicant", example = "John Smith")
    private String applicantName;
}