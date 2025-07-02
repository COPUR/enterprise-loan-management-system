package com.bank.loan.loan.ai.infrastructure.web;

import com.bank.loan.loan.ai.application.port.in.AnalyzeLoanRequestCommand;
import com.bank.loan.loan.ai.application.port.in.AnalyzeLoanRequestUseCase;
import com.bank.loan.loan.ai.application.port.in.ProcessNaturalLanguageRequestCommand;
import com.bank.loan.loan.ai.application.port.in.ProcessNaturalLanguageRequestUseCase;
import com.bank.loan.loan.ai.domain.model.LoanAnalysisResult;
import com.bank.loan.loan.ai.infrastructure.web.dto.AiLoanAnalysisRequest;
import com.bank.loan.loan.ai.infrastructure.web.dto.AiLoanAnalysisResponse;
import com.bank.loan.loan.ai.infrastructure.web.dto.NaturalLanguageAnalysisRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered loan analysis
 */
@RestController
@RequestMapping("/api/v1/ai/loan-analysis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Loan Analysis", description = "AI-powered loan analysis and decision support")
public class AiLoanAnalysisController {

    private final AnalyzeLoanRequestUseCase analyzeLoanRequestUseCase;
    private final ProcessNaturalLanguageRequestUseCase processNaturalLanguageRequestUseCase;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze loan request using AI", 
               description = "Perform comprehensive AI analysis on structured loan application data")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('UNDERWRITER')")
    public ResponseEntity<AiLoanAnalysisResponse> analyzeLoan(@Valid @RequestBody AiLoanAnalysisRequest request) {
        log.info("Received AI loan analysis request for applicant: {}", request.getApplicantId());

        try {
            // Convert to command
            AnalyzeLoanRequestCommand command = request.toCommand();
            
            // Perform analysis
            LoanAnalysisResult result = analyzeLoanRequestUseCase.analyze(command);
            
            // Convert to response
            AiLoanAnalysisResponse response = AiLoanAnalysisResponse.from(result);
            
            log.info("Completed AI loan analysis for applicant: {} with recommendation: {}", 
                    request.getApplicantId(), result.getOverallRecommendation());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze loan request for applicant: {}", request.getApplicantId(), e);
            throw e;
        }
    }

    @PostMapping("/analyze/natural-language")
    @Operation(summary = "Analyze natural language loan request", 
               description = "Process unstructured loan request text and perform AI analysis")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('UNDERWRITER')")
    public ResponseEntity<AiLoanAnalysisResponse> analyzeNaturalLanguage(
            @Valid @RequestBody NaturalLanguageAnalysisRequest request) {
        
        log.info("Received natural language loan analysis request for applicant: {}", request.getApplicantId());

        try {
            // Process natural language to structured data
            ProcessNaturalLanguageRequestCommand nlpCommand = new ProcessNaturalLanguageRequestCommand(
                request.getRequestId(),
                request.getNaturalLanguageText(),
                request.getApplicantId(),
                request.getApplicantName()
            );
            
            AnalyzeLoanRequestCommand structuredCommand = 
                processNaturalLanguageRequestUseCase.processNaturalLanguage(nlpCommand);
            
            // Perform analysis on structured data
            LoanAnalysisResult result = analyzeLoanRequestUseCase.analyze(structuredCommand);
            
            // Convert to response
            AiLoanAnalysisResponse response = AiLoanAnalysisResponse.from(result);
            
            log.info("Completed natural language loan analysis for applicant: {} with recommendation: {}", 
                    request.getApplicantId(), result.getOverallRecommendation());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to analyze natural language loan request for applicant: {}", 
                    request.getApplicantId(), e);
            throw e;
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Check AI service health", 
               description = "Verify that AI services are available and functioning")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('UNDERWRITER') or hasRole('ADMIN')")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI Loan Analysis service is running");
    }
}