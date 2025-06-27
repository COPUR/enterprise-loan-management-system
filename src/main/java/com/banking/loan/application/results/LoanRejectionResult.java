package com.banking.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;

public record LoanRejectionResult(
    String loanId,
    List<String> rejectionReasons,
    List<String> alternativeOptions,
    String appealProcess,
    String status,
    LocalDateTime rejectedAt,
    String rejecterId,
    String message
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String loanId;
        private List<String> rejectionReasons;
        private List<String> alternativeOptions;
        private String appealProcess;
        private String status = "REJECTED";
        private LocalDateTime rejectedAt = LocalDateTime.now();
        private String rejecterId;
        private String message;
        
        public Builder loanId(String loanId) {
            this.loanId = loanId;
            return this;
        }
        
        public Builder rejectionReasons(List<String> rejectionReasons) {
            this.rejectionReasons = rejectionReasons;
            return this;
        }
        
        public Builder alternativeOptions(List<String> alternativeOptions) {
            this.alternativeOptions = alternativeOptions;
            return this;
        }
        
        public Builder appealProcess(String appealProcess) {
            this.appealProcess = appealProcess;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder rejectedAt(LocalDateTime rejectedAt) {
            this.rejectedAt = rejectedAt;
            return this;
        }
        
        public Builder rejecterId(String rejecterId) {
            this.rejecterId = rejecterId;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public LoanRejectionResult build() {
            return new LoanRejectionResult(
                loanId, rejectionReasons, alternativeOptions, appealProcess,
                status, rejectedAt, rejecterId, message
            );
        }
    }
}