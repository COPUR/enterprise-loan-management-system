package com.bank.loan.loan.ai.application.port.in;

import com.bank.loan.loan.sharedkernel.application.command.Command;
import lombok.Value;

/**
 * Command to process natural language loan requests
 */
@Value
public class ProcessNaturalLanguageRequestCommand implements Command {
    
    String requestId;
    String naturalLanguageText;
    String applicantId;
    String applicantName;

    /**
     * Validate that required fields are present
     */
    public void validate() {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID is required");
        }
        if (naturalLanguageText == null || naturalLanguageText.trim().isEmpty()) {
            throw new IllegalArgumentException("Natural language text is required");
        }
        if (applicantId == null || applicantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Applicant ID is required");
        }
    }
}