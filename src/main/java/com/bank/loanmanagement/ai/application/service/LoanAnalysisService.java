package com.bank.loanmanagement.ai.application.service;

import com.bank.loanmanagement.ai.application.port.in.AnalyzeLoanRequestCommand;
import com.bank.loanmanagement.ai.application.port.in.AnalyzeLoanRequestUseCase;
import com.bank.loanmanagement.ai.application.port.out.AiLoanAnalysisPort;
import com.bank.loanmanagement.ai.application.port.out.FraudDetectionPort;
import com.bank.loanmanagement.ai.application.port.out.LoanAnalysisRepository;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisRequest;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisResult;
import com.bank.loanmanagement.ai.domain.service.LoanAnalysisValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service for AI-powered loan analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanAnalysisService implements AnalyzeLoanRequestUseCase {

    private final AiLoanAnalysisPort aiLoanAnalysisPort;
    private final FraudDetectionPort fraudDetectionPort;
    private final LoanAnalysisRepository repository;
    private final LoanAnalysisValidationService validationService;

    @Override
    public LoanAnalysisResult analyze(AnalyzeLoanRequestCommand command) {
        log.info("Starting AI loan analysis for request: {}", command.getRequestId());
        
        // Validate command
        validateCommand(command);
        
        // Check AI service availability
        if (!aiLoanAnalysisPort.isAvailable()) {
            throw new IllegalStateException("AI loan analysis service is not available");
        }

        try {
            // Create domain request
            LoanAnalysisRequest request = createDomainRequest(command);
            
            // Validate business rules before processing (Guardrail #1)
            LoanAnalysisValidationService.BusinessRuleValidationResult businessValidation = 
                validationService.validateBusinessRules(request);
            
            if (!businessValidation.isValid()) {
                log.warn("Business rule validation failed for request: {} - Violations: {}", 
                        command.getRequestId(), businessValidation.violations());
                throw new BusinessRuleViolationException("Business rule violations: " + 
                        String.join(", ", businessValidation.violations()));
            }
            
            // Save request
            request = repository.save(request);
            log.debug("Saved loan analysis request: {}", request.getId());

            // Mark as processing
            request.markAsProcessing();
            repository.save(request);

            // Perform AI analysis
            long startTime = System.currentTimeMillis();
            LoanAnalysisResult result = aiLoanAnalysisPort.performAnalysis(request);
            long processingTime = System.currentTimeMillis() - startTime;

            // Validate AI analysis result (Guardrail #2)
            LoanAnalysisValidationService.LoanAnalysisValidationResult analysisValidation = 
                validationService.validateAnalysisResult(request, result);
            
            if (!analysisValidation.isValid()) {
                log.warn("AI analysis validation failed for request: {} - Issues: {}", 
                        command.getRequestId(), analysisValidation.issues().size());
                
                // Add validation issues to result for manual review
                for (var issue : analysisValidation.issues()) {
                    if (issue.severity() == LoanAnalysisValidationService.ValidationSeverity.CRITICAL) {
                        throw new AnalysisValidationException("Critical validation issue: " + issue.description());
                    }
                }
            }
            
            // Add additional risk factors identified by validation
            for (var riskFactor : analysisValidation.additionalRiskFactors()) {
                result.addRiskFactor(riskFactor);
            }

            // Add fraud detection if available
            if (fraudDetectionPort.isAvailable()) {
                enrichWithFraudAnalysis(request, result);
            }

            // Set processing metadata
            result.setProcessingMetadata(aiLoanAnalysisPort.getModelVersion(), processingTime);

            // Save result
            result = repository.save(result);
            
            // Mark request as completed
            request.markAsCompleted();
            repository.save(request);

            log.info("Completed AI loan analysis for request: {} with recommendation: {} (Validation: {})", 
                    command.getRequestId(), result.getOverallRecommendation(), 
                    analysisValidation.isValid() ? "PASS" : "ISSUES");

            return result;

        } catch (Exception e) {
            log.error("Failed to analyze loan request: {}", command.getRequestId(), e);
            
            // Try to mark request as failed if it exists
            repository.findRequestById(LoanAnalysisRequestId.of(command.getRequestId()))
                    .ifPresent(request -> {
                        request.markAsFailed();
                        repository.save(request);
                    });
            
            throw new LoanAnalysisException("Failed to analyze loan request: " + e.getMessage(), e);
        }
    }

    private void validateCommand(AnalyzeLoanRequestCommand command) {
        if (!command.hasCompleteStructuredData()) {
            throw new IllegalArgumentException("Incomplete loan request data. Required fields missing.");
        }
    }

    private LoanAnalysisRequest createDomainRequest(AnalyzeLoanRequestCommand command) {
        // Create request ID
        LoanAnalysisRequestId requestId = LoanAnalysisRequestId.of(command.getRequestId());
        
        if (command.requiresNlpProcessing()) {
            return LoanAnalysisRequest.createForNlpProcessing(
                requestId,
                command.getApplicantId(),
                command.getApplicantName(),
                command.getNaturalLanguageRequest()
            );
        } else {
            return LoanAnalysisRequest.createStructured(
                requestId,
                command.getRequestedAmount(),
                command.getApplicantId(),
                command.getApplicantName(),
                command.getMonthlyIncome(),
                command.getMonthlyExpenses(),
                command.getEmploymentType(),
                command.getEmploymentTenureMonths(),
                command.getLoanPurpose(),
                command.getRequestedTermMonths(),
                command.getCurrentDebt(),
                command.getCreditScore(),
                command.getAdditionalData()
            );
        }
    }

    private void enrichWithFraudAnalysis(LoanAnalysisRequest request, LoanAnalysisResult result) {
        try {
            FraudDetectionPort.FraudAnalysisResult fraudAnalysis = 
                fraudDetectionPort.analyzeFraudRisk(request);
            
            if (fraudAnalysis.hasFraudRisk()) {
                result.setFraudRiskIndicators(fraudAnalysis.riskIndicators());
                log.warn("Fraud risk detected for request: {} - Score: {}", 
                        request.getId(), fraudAnalysis.fraudScore());
            }
        } catch (Exception e) {
            log.warn("Failed to perform fraud analysis for request: {}", request.getId(), e);
            // Continue without fraud analysis rather than failing the entire process
        }
    }

    /**
     * Custom exception for loan analysis failures
     */
    public static class LoanAnalysisException extends RuntimeException {
        public LoanAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Custom exception for business rule violations
     */
    public static class BusinessRuleViolationException extends RuntimeException {
        public BusinessRuleViolationException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for analysis validation failures
     */
    public static class AnalysisValidationException extends RuntimeException {
        public AnalysisValidationException(String message) {
            super(message);
        }
    }
}