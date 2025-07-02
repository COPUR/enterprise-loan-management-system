package com.bank.loanmanagement.loan.infrastructure.web;

import com.bank.loanmanagement.loan.domain.loan.LoanRecommendationUseCase;
import com.bank.loanmanagement.loan.domain.loan.LoanRecommendationUseCase.LoanRecommendationCommand;
import com.bank.loanmanagement.loan.domain.loan.LoanRecommendationUseCase.LoanRecommendationResult;
import com.bank.loanmanagement.loan.infrastructure.web.dto.LoanRecommendationRequestDto;
import com.bank.loanmanagement.loan.infrastructure.web.dto.LoanRecommendationResponseDto;
import com.bank.loanmanagement.loan.infrastructure.web.dto.ErrorResponseDto;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

/**
 * AI-Powered Loan Recommendations API
 * 
 * Provides intelligent, personalized loan recommendations using machine learning
 * and comprehensive financial analysis following clean hexagonal architecture.
 */
@RestController
@RequestMapping("/api/v1/loans/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Loan Recommendations", 
     description = "AI-powered loan recommendation engine providing personalized loan offers based on comprehensive financial analysis")
@SecurityRequirement(name = "bearerAuth")
public class LoanRecommendationController {

    private final LoanRecommendationUseCase loanRecommendationUseCase;

    @Operation(
        summary = "Generate AI-powered loan recommendations",
        description = "Generates personalized loan recommendations using advanced AI analysis. " +
                     "This endpoint analyzes the customer's comprehensive financial profile using " +
                     "traditional risk assessment metrics, AI-powered behavioral analysis, " +
                     "real-time market conditions and regulatory compliance validation.",
        operationId = "generateLoanRecommendations"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Loan recommendations generated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoanRecommendationResponseDto.class),
                examples = @ExampleObject(
                    name = "Successful recommendation",
                    summary = "Example of successful loan recommendations",
                    value = "{ \"customerId\": \"CUST-12345\", \"recommendations\": [{ \"id\": \"OFFER-ABC123\", \"loanType\": \"PERSONAL\", \"amount\": 25000.00, \"currency\": \"USD\", \"interestRatePercentage\": 7.25, \"termMonths\": 60, \"monthlyPayment\": 495.87, \"riskLevel\": \"MEDIUM\", \"reasoning\": \"Recommended personal loan with competitive rate\", \"confidenceScore\": 0.92, \"features\": [\"Competitive Rate\", \"Fast Approval\"] }], \"riskAssessment\": { \"riskLevel\": \"MEDIUM\", \"riskScore\": 35, \"defaultProbability\": 0.08, \"riskFactors\": [\"DTI ratio above optimal\"], \"mitigatingFactors\": [\"Excellent credit score\"], \"confidenceLevel\": 0.89 }, \"analysisVersion\": \"v2.1\", \"generatedAt\": \"2024-12-20T10:30:00Z\" }"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or insufficient data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class),
                examples = @ExampleObject(
                    name = "Validation error",
                    value = "{ \"error\": \"VALIDATION_ERROR\", \"message\": \"Customer does not meet basic lending criteria\", \"details\": [\"Credit score below minimum threshold\"], \"timestamp\": \"2024-12-20T10:30:00Z\" }"
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many requests - rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - AI service unavailable",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<LoanRecommendationResponseDto> getRecommendations(
            @Parameter(
                description = "Comprehensive loan recommendation request with customer financial profile",
                required = true,
                schema = @Schema(implementation = LoanRecommendationRequestDto.class)
            )
            @Valid @RequestBody LoanRecommendationRequestDto requestDto) {
        
        log.info("Generating loan recommendations for customer: {}", 
                requestDto.customerId());
        
        try {
            // Convert DTO to domain command
            LoanRecommendationCommand command = requestDto.toDomainCommand();
            
            // Execute use case
            LoanRecommendationResult result = loanRecommendationUseCase.generateRecommendations(command);
            
            // Convert domain result to DTO
            LoanRecommendationResponseDto responseDto = LoanRecommendationResponseDto.fromDomainResult(result);
            
            return ResponseEntity.ok(responseDto);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid loan recommendation request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error generating loan recommendations for customer: {}", requestDto.customerId(), e);
            throw new RuntimeException("Failed to generate loan recommendations", e);
        }
    }

    @Operation(
        summary = "Retrieve latest loan recommendations for customer",
        description = "Retrieves the most recent loan recommendations generated for a specific customer. " +
                     "This endpoint allows customers and advisors to review previously generated recommendations, " +
                     "track recommendation history, and compare different recommendation sessions.",
        operationId = "getLatestLoanRecommendations"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Latest recommendations retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoanRecommendationResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "204",
            description = "No recent recommendations found for this customer"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions to access customer data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDto.class)
            )
        )
    })
    @GetMapping("/{customerId}/latest")
    public ResponseEntity<LoanRecommendationResponseDto> getLatestRecommendations(
            @Parameter(
                description = "Unique customer identifier",
                required = true,
                example = "CUST-12345",
                schema = @Schema(type = "string", pattern = "^CUST-[A-Z0-9]+$")
            )
            @PathVariable String customerId) {
        
        log.info("Retrieving latest recommendations for customer: {}", customerId);
        
        // Mock implementation - in production, retrieve from database
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException e) {
        ErrorResponseDto error = ErrorResponseDto.of("VALIDATION_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(RuntimeException e) {
        log.error("Unexpected error in loan recommendations", e);
        ErrorResponseDto error = ErrorResponseDto.of("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}