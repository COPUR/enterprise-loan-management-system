package com.bank.loan.loan.ai.application.port.out;

import com.bank.loan.loan.ai.application.port.in.AnalyzeLoanRequestCommand;

/**
 * Output port for natural language processing
 */
public interface NaturalLanguageProcessingPort {

    /**
     * Extract structured data from natural language loan request
     * 
     * @param naturalLanguageText the unstructured text
     * @param applicantId the applicant identifier
     * @param applicantName the applicant name
     * @return structured loan request command
     */
    AnalyzeLoanRequestCommand extractStructuredData(String naturalLanguageText, String applicantId, String applicantName);

    /**
     * Check if NLP service is available
     * 
     * @return true if NLP service is ready
     */
    boolean isAvailable();
}