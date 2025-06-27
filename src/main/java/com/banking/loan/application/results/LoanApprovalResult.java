package com.banking.loan.application.results;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LoanApprovalResult(
    String loanId,
    BigDecimal approvedAmount,
    BigDecimal interestRate,
    LocalDate firstPaymentDate,
    BigDecimal monthlyInstallment,
    BigDecimal totalPayableAmount,
    String loanAgreementNumber,
    String status,
    LocalDateTime approvedAt,
    String approverId,
    List<String> conditions,
    String message
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String loanId;
        private BigDecimal approvedAmount;
        private BigDecimal interestRate;
        private LocalDate firstPaymentDate;
        private BigDecimal monthlyInstallment;
        private BigDecimal totalPayableAmount;
        private String loanAgreementNumber;
        private String status = "APPROVED";
        private LocalDateTime approvedAt = LocalDateTime.now();
        private String approverId;
        private List<String> conditions;
        private String message;
        
        public Builder loanId(String loanId) {
            this.loanId = loanId;
            return this;
        }
        
        public Builder approvedAmount(BigDecimal approvedAmount) {
            this.approvedAmount = approvedAmount;
            return this;
        }
        
        public Builder interestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
            return this;
        }
        
        public Builder firstPaymentDate(LocalDate firstPaymentDate) {
            this.firstPaymentDate = firstPaymentDate;
            return this;
        }
        
        public Builder monthlyInstallment(BigDecimal monthlyInstallment) {
            this.monthlyInstallment = monthlyInstallment;
            return this;
        }
        
        public Builder totalPayableAmount(BigDecimal totalPayableAmount) {
            this.totalPayableAmount = totalPayableAmount;
            return this;
        }
        
        public Builder loanAgreementNumber(String loanAgreementNumber) {
            this.loanAgreementNumber = loanAgreementNumber;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder approvedAt(LocalDateTime approvedAt) {
            this.approvedAt = approvedAt;
            return this;
        }
        
        public Builder approverId(String approverId) {
            this.approverId = approverId;
            return this;
        }
        
        public Builder conditions(List<String> conditions) {
            this.conditions = conditions;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public LoanApprovalResult build() {
            return new LoanApprovalResult(
                loanId, approvedAmount, interestRate, firstPaymentDate,
                monthlyInstallment, totalPayableAmount, loanAgreementNumber,
                status, approvedAt, approverId, conditions, message
            );
        }
    }
}