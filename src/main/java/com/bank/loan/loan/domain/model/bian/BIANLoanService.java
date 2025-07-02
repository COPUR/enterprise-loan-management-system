package com.bank.loanmanagement.loan.domain.model.bian;

import com.bank.loanmanagement.loan.domain.model.bian.BerlinGroupAmount;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * BIAN Consumer Loan Service Domain Implementation
 * 
 * Implements:
 * - BIAN Service Domain Model for Consumer Loan
 * - ISO 20022 messaging standards
 * - Berlin Group PSD2 compliance
 * - Functional patterns: Fulfill, Maintain, Provide
 * - Control Records and Behavior Qualifiers
 */
@Data
@Builder
@Jacksonized
public class BIANLoanService implements BIANServiceDomain {
    
    /**
     * Consumer Loan Service Domain Reference (SDR)
     * Unique identifier for the loan service domain instance
     */
    @JsonProperty("consumerLoanServicingSessionReference")
    private String consumerLoanServicingSessionReference;
    
    /**
     * Loan Agreement Control Record
     * Defines the overall loan arrangement and its lifecycle
     */
    @JsonProperty("loanAgreement")
    private BIANLoanAgreement loanAgreement;
    
    /**
     * Loan Origination Control Record
     * Manages the loan application and approval process
     */
    @JsonProperty("loanOrigination")
    private BIANLoanOrigination loanOrigination;
    
    /**
     * Loan Fulfillment Control Record
     * Manages loan disbursement and activation
     */
    @JsonProperty("loanFulfillment")
    private BIANLoanFulfillment loanFulfillment;
    
    /**
     * Loan Maintenance Control Record
     * Ongoing loan management and servicing
     */
    @JsonProperty("loanMaintenance")
    private BIANLoanMaintenance loanMaintenance;
    
    /**
     * Interest Calculation Control Record
     * Interest accrual and calculation management
     */
    @JsonProperty("interestCalculation")
    private BIANInterestCalculation interestCalculation;
    
    /**
     * Payment Processing Control Record
     * Payment collection and allocation
     */
    @JsonProperty("paymentProcessing")
    private BIANPaymentProcessing paymentProcessing;
    
    /**
     * Risk Assessment Control Record
     * Ongoing risk monitoring and assessment
     */
    @JsonProperty("riskAssessment")
    private BIANRiskAssessment riskAssessment;
    
    /**
     * Compliance Monitoring Control Record
     * Regulatory compliance tracking
     */
    @JsonProperty("complianceMonitoring")
    private BIANComplianceMonitoring complianceMonitoring;
    
    /**
     * BIAN Loan Agreement Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANLoanAgreement {
        
        @JsonProperty("loanAgreementReference")
        private String loanAgreementReference;
        
        @JsonProperty("customerReference")
        private String customerReference;
        
        @JsonProperty("loanType")
        private LoanType loanType;
        
        @JsonProperty("loanAmount")
        private BerlinGroupAmount loanAmount;
        
        @JsonProperty("loanCurrency")
        private String loanCurrency;
        
        @JsonProperty("loanRateSetting")
        private BIANLoanRateSetting loanRateSetting;
        
        @JsonProperty("loanTerms")
        private BIANLoanTerms loanTerms;
        
        @JsonProperty("collateralAllocation")
        private List<BIANCollateralAllocation> collateralAllocation;
        
        @JsonProperty("loanStatus")
        private LoanStatus loanStatus;
        
        @JsonProperty("agreementDate")
        private LocalDate agreementDate;
        
        @JsonProperty("maturityDate")
        private LocalDate maturityDate;
        
        public enum LoanType {
            @JsonProperty("personal")
            PERSONAL,
            
            @JsonProperty("mortgage")
            MORTGAGE,
            
            @JsonProperty("auto")
            AUTO,
            
            @JsonProperty("business")
            BUSINESS,
            
            @JsonProperty("student")
            STUDENT,
            
            @JsonProperty("creditCard")
            CREDIT_CARD,
            
            @JsonProperty("lineOfCredit")
            LINE_OF_CREDIT
        }
        
        public enum LoanStatus {
            @JsonProperty("applied")
            APPLIED,
            
            @JsonProperty("approved")
            APPROVED,
            
            @JsonProperty("active")
            ACTIVE,
            
            @JsonProperty("suspended")
            SUSPENDED,
            
            @JsonProperty("closed")
            CLOSED,
            
            @JsonProperty("defaulted")
            DEFAULTED,
            
            @JsonProperty("chargedOff")
            CHARGED_OFF
        }
    }
    
    /**
     * BIAN Loan Rate Setting
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANLoanRateSetting {
        
        @JsonProperty("rateType")
        private RateType rateType;
        
        @JsonProperty("baseRate")
        private BigDecimal baseRate;
        
        @JsonProperty("spread")
        private BigDecimal spread;
        
        @JsonProperty("effectiveRate")
        private BigDecimal effectiveRate;
        
        @JsonProperty("rateResetFrequency")
        private String rateResetFrequency;
        
        @JsonProperty("rateResetDate")
        private LocalDate rateResetDate;
        
        @JsonProperty("rateCap")
        private BigDecimal rateCap;
        
        @JsonProperty("rateFloor")
        private BigDecimal rateFloor;
        
        public enum RateType {
            @JsonProperty("fixed")
            FIXED,
            
            @JsonProperty("variable")
            VARIABLE,
            
            @JsonProperty("stepped")
            STEPPED,
            
            @JsonProperty("promotional")
            PROMOTIONAL
        }
    }
    
    /**
     * BIAN Loan Terms
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANLoanTerms {
        
        @JsonProperty("repaymentSchedule")
        private RepaymentSchedule repaymentSchedule;
        
        @JsonProperty("paymentFrequency")
        private PaymentFrequency paymentFrequency;
        
        @JsonProperty("paymentAmount")
        private BerlinGroupAmount paymentAmount;
        
        @JsonProperty("paymentHolidays")
        private List<BIANPaymentHoliday> paymentHolidays;
        
        @JsonProperty("prepaymentTerms")
        private BIANPrepaymentTerms prepaymentTerms;
        
        @JsonProperty("defaultProvisions")
        private BIANDefaultProvisions defaultProvisions;
        
        public enum RepaymentSchedule {
            @JsonProperty("equalInstallments")
            EQUAL_INSTALLMENTS,
            
            @JsonProperty("equalPrincipal")
            EQUAL_PRINCIPAL,
            
            @JsonProperty("interestOnly")
            INTEREST_ONLY,
            
            @JsonProperty("balloonPayment")
            BALLOON_PAYMENT,
            
            @JsonProperty("graduated")
            GRADUATED
        }
        
        public enum PaymentFrequency {
            @JsonProperty("weekly")
            WEEKLY,
            
            @JsonProperty("biweekly")
            BIWEEKLY,
            
            @JsonProperty("monthly")
            MONTHLY,
            
            @JsonProperty("quarterly")
            QUARTERLY,
            
            @JsonProperty("semiannual")
            SEMIANNUAL,
            
            @JsonProperty("annual")
            ANNUAL
        }
    }
    
    /**
     * BIAN Loan Origination Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANLoanOrigination {
        
        @JsonProperty("loanApplicationReference")
        private String loanApplicationReference;
        
        @JsonProperty("applicationDate")
        private LocalDateTime applicationDate;
        
        @JsonProperty("applicantDetails")
        private BIANApplicantDetails applicantDetails;
        
        @JsonProperty("underwritingAssessment")
        private BIANUnderwritingAssessment underwritingAssessment;
        
        @JsonProperty("creditDecision")
        private BIANCreditDecision creditDecision;
        
        @JsonProperty("documentationRequirements")
        private List<BIANDocumentationRequirement> documentationRequirements;
        
        @JsonProperty("approvalConditions")
        private List<BIANApprovalCondition> approvalConditions;
    }
    
    /**
     * BIAN Loan Fulfillment Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANLoanFulfillment {
        
        @JsonProperty("fulfillmentReference")
        private String fulfillmentReference;
        
        @JsonProperty("disbursementInstructions")
        private BIANDisbursementInstructions disbursementInstructions;
        
        @JsonProperty("fundingArrangement")
        private BIANFundingArrangement fundingArrangement;
        
        @JsonProperty("disbursementRecord")
        private List<BIANDisbursementRecord> disbursementRecord;
        
        @JsonProperty("activationDate")
        private LocalDate activationDate;
        
        @JsonProperty("firstPaymentDate")
        private LocalDate firstPaymentDate;
    }
    
    /**
     * BIAN Loan Maintenance Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANLoanMaintenance {
        
        @JsonProperty("maintenanceReference")
        private String maintenanceReference;
        
        @JsonProperty("accountBalance")
        private BIANAccountBalance accountBalance;
        
        @JsonProperty("paymentSchedule")
        private List<BIANPaymentScheduleEntry> paymentSchedule;
        
        @JsonProperty("transactionLog")
        private List<BIANTransactionLogEntry> transactionLog;
        
        @JsonProperty("accountStatements")
        private List<BIANAccountStatement> accountStatements;
        
        @JsonProperty("customerCommunications")
        private List<BIANCustomerCommunication> customerCommunications;
    }
    
    /**
     * BIAN Interest Calculation Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANInterestCalculation {
        
        @JsonProperty("calculationReference")
        private String calculationReference;
        
        @JsonProperty("interestAccrualMethod")
        private InterestAccrualMethod interestAccrualMethod;
        
        @JsonProperty("interestRateTerms")
        private BIANInterestRateTerms interestRateTerms;
        
        @JsonProperty("accrualRecord")
        private List<BIANAccrualRecord> accrualRecord;
        
        @JsonProperty("compoundingFrequency")
        private CompoundingFrequency compoundingFrequency;
        
        @JsonProperty("dayCountBasis")
        private DayCountBasis dayCountBasis;
        
        public enum InterestAccrualMethod {
            @JsonProperty("simple")
            SIMPLE,
            
            @JsonProperty("compound")
            COMPOUND,
            
            @JsonProperty("continuous")
            CONTINUOUS
        }
        
        public enum CompoundingFrequency {
            @JsonProperty("daily")
            DAILY,
            
            @JsonProperty("monthly")
            MONTHLY,
            
            @JsonProperty("quarterly")
            QUARTERLY,
            
            @JsonProperty("semiannual")
            SEMIANNUAL,
            
            @JsonProperty("annual")
            ANNUAL
        }
        
        public enum DayCountBasis {
            @JsonProperty("actual365")
            ACTUAL_365,
            
            @JsonProperty("actual360")
            ACTUAL_360,
            
            @JsonProperty("thirty360")
            THIRTY_360,
            
            @JsonProperty("actualActual")
            ACTUAL_ACTUAL
        }
    }
    
    /**
     * BIAN Payment Processing Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANPaymentProcessing {
        
        @JsonProperty("paymentProcessingReference")
        private String paymentProcessingReference;
        
        @JsonProperty("paymentMethod")
        private PaymentMethod paymentMethod;
        
        @JsonProperty("paymentInstructions")
        private BIANPaymentInstructions paymentInstructions;
        
        @JsonProperty("paymentExecutionLog")
        private List<BIANPaymentExecution> paymentExecutionLog;
        
        @JsonProperty("paymentAllocation")
        private BIANPaymentAllocation paymentAllocation;
        
        @JsonProperty("overdueManagement")
        private BIANOverdueManagement overdueManagement;
        
        public enum PaymentMethod {
            @JsonProperty("directDebit")
            DIRECT_DEBIT,
            
            @JsonProperty("standingOrder")
            STANDING_ORDER,
            
            @JsonProperty("onlinePayment")
            ONLINE_PAYMENT,
            
            @JsonProperty("check")
            CHECK,
            
            @JsonProperty("wire")
            WIRE,
            
            @JsonProperty("ach")
            ACH,
            
            @JsonProperty("card")
            CARD
        }
    }
    
    /**
     * BIAN Risk Assessment Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANRiskAssessment {
        
        @JsonProperty("riskAssessmentReference")
        private String riskAssessmentReference;
        
        @JsonProperty("creditRiskAssessment")
        private BIANCreditRiskAssessment creditRiskAssessment;
        
        @JsonProperty("marketRiskAssessment")
        private BIANMarketRiskAssessment marketRiskAssessment;
        
        @JsonProperty("operationalRiskAssessment")
        private BIANOperationalRiskAssessment operationalRiskAssessment;
        
        @JsonProperty("portfolioRiskAssessment")
        private BIANPortfolioRiskAssessment portfolioRiskAssessment;
        
        @JsonProperty("riskMitigationActions")
        private List<BIANRiskMitigationAction> riskMitigationActions;
        
        @JsonProperty("riskReporting")
        private BIANRiskReporting riskReporting;
    }
    
    /**
     * BIAN Compliance Monitoring Control Record
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANComplianceMonitoring {
        
        @JsonProperty("complianceMonitoringReference")
        private String complianceMonitoringReference;
        
        @JsonProperty("regulatoryRequirements")
        private List<BIANRegulatoryRequirement> regulatoryRequirements;
        
        @JsonProperty("complianceChecks")
        private List<BIANComplianceCheck> complianceChecks;
        
        @JsonProperty("auditTrail")
        private BIANAuditTrail auditTrail;
        
        @JsonProperty("reportingObligations")
        private List<BIANReportingObligation> reportingObligations;
        
        @JsonProperty("breachManagement")
        private BIANBreachManagement breachManagement;
    }
    
    // Implementation of BIANServiceDomain interface
    
    @Override
    public String getServiceDomainReference() {
        return consumerLoanServicingSessionReference;
    }
    
    @Override
    public String getServiceDomainType() {
        return "ConsumerLoan";
    }
    
    public String getBIANFunctionalPattern() {
        return "Fulfill"; // Consumer Loan follows the Fulfill pattern
    }
    
    public String getBIANBusinessArea() {
        return "Customer Products & Services";
    }
    
    /**
     * Get the current loan status
     */
    public BIANLoanAgreement.LoanStatus getCurrentLoanStatus() {
        return loanAgreement != null ? loanAgreement.getLoanStatus() : null;
    }
    
    /**
     * Get outstanding principal balance
     */
    public BigDecimal getOutstandingPrincipal() {
        if (loanMaintenance != null && loanMaintenance.getAccountBalance() != null) {
            return loanMaintenance.getAccountBalance().getPrincipalBalance();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get total outstanding balance (principal + interest + fees)
     */
    public BigDecimal getTotalOutstandingBalance() {
        if (loanMaintenance != null && loanMaintenance.getAccountBalance() != null) {
            var balance = loanMaintenance.getAccountBalance();
            return balance.getPrincipalBalance()
                .add(balance.getAccruedInterest())
                .add(balance.getOutstandingFees());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Check if loan is in good standing
     */
    public boolean isInGoodStanding() {
        var status = getCurrentLoanStatus();
        return status == BIANLoanAgreement.LoanStatus.ACTIVE ||
               status == BIANLoanAgreement.LoanStatus.APPROVED;
    }
    
    /**
     * Get next payment due date
     */
    public LocalDate getNextPaymentDueDate() {
        if (loanMaintenance != null && loanMaintenance.getPaymentSchedule() != null) {
            return loanMaintenance.getPaymentSchedule().stream()
                .filter(entry -> !entry.isPaid())
                .map(BIANPaymentScheduleEntry::getDueDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        }
        return null;
    }
    
    /**
     * Calculate days past due
     */
    public int getDaysPastDue() {
        var nextDueDate = getNextPaymentDueDate();
        if (nextDueDate != null && nextDueDate.isBefore(LocalDate.now())) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(nextDueDate, LocalDate.now());
        }
        return 0;
    }
    
    // Additional helper classes would be defined here...
    
    @Data
    @Builder
    @Jacksonized
    public static class BIANAccountBalance {
        private BigDecimal principalBalance;
        private BigDecimal accruedInterest;
        private BigDecimal outstandingFees;
        private BigDecimal escrowBalance;
        private LocalDate balanceDate;
    }
    
    @Data
    @Builder
    @Jacksonized
    public static class BIANPaymentScheduleEntry {
        private String entryReference;
        private LocalDate dueDate;
        private BerlinGroupAmount paymentAmount;
        private BerlinGroupAmount principalAmount;
        private BerlinGroupAmount interestAmount;
        private BerlinGroupAmount feeAmount;
        private boolean paid;
        private LocalDate paidDate;
    }
    
    // Additional classes for collateral, disbursement, etc. would be implemented...
}