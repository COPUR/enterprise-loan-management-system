package com.bank.loan.loan.ai.application.port.in;

/**
 * Use case port for processing natural language loan requests
 */
public interface ProcessNaturalLanguageRequestUseCase {

    /**
     * Process natural language text and extract structured loan request data
     * 
     * @param command the natural language processing command
     * @return structured command ready for loan analysis
     */
    AnalyzeLoanRequestCommand processNaturalLanguage(ProcessNaturalLanguageRequestCommand command);
}