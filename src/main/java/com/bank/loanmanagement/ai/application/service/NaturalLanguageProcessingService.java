package com.bank.loanmanagement.ai.application.service;

import com.bank.loanmanagement.ai.application.port.in.AnalyzeLoanRequestCommand;
import com.bank.loanmanagement.ai.application.port.in.ProcessNaturalLanguageRequestCommand;
import com.bank.loanmanagement.ai.application.port.in.ProcessNaturalLanguageRequestUseCase;
import com.bank.loanmanagement.ai.application.port.out.NaturalLanguageProcessingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for natural language processing of loan requests
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NaturalLanguageProcessingService implements ProcessNaturalLanguageRequestUseCase {

    private final NaturalLanguageProcessingPort nlpPort;

    @Override
    public AnalyzeLoanRequestCommand processNaturalLanguage(ProcessNaturalLanguageRequestCommand command) {
        log.info("Processing natural language request: {}", command.getRequestId());
        
        // Validate command
        command.validate();
        
        // Check NLP service availability
        if (!nlpPort.isAvailable()) {
            throw new IllegalStateException("Natural Language Processing service is not available");
        }

        try {
            // Extract structured data from natural language
            AnalyzeLoanRequestCommand structuredCommand = nlpPort.extractStructuredData(
                command.getNaturalLanguageText(),
                command.getApplicantId(),
                command.getApplicantName()
            );

            log.info("Successfully processed natural language request: {} into structured data", 
                    command.getRequestId());

            return structuredCommand;

        } catch (Exception e) {
            log.error("Failed to process natural language request: {}", command.getRequestId(), e);
            throw new NaturalLanguageProcessingException(
                "Failed to process natural language request: " + e.getMessage(), e);
        }
    }

    /**
     * Custom exception for NLP processing failures
     */
    public static class NaturalLanguageProcessingException extends RuntimeException {
        public NaturalLanguageProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}