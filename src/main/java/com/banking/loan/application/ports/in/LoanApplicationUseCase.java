package com.banking.loan.application.ports.in;

import com.banking.loan.application.commands.*;
import com.banking.loan.application.queries.*;
import java.util.List;

/**
 * Loan Application Use Case - Hexagonal Architecture Port
 * Defines the contract for loan application operations
 */
public interface LoanApplicationUseCase {
    
    /**
     * Submit a new loan application
     */
    LoanApplicationResult submitLoanApplication(SubmitLoanApplicationCommand command);
    
    /**
     * Approve a loan application
     */
    LoanApprovalResult approveLoan(ApproveLoanCommand command);
    
    /**
     * Reject a loan application
     */
    LoanRejectionResult rejectLoan(RejectLoanCommand command);
    
    /**
     * Get detailed information about a loan
     */
    LoanDetails getLoanDetails(GetLoanDetailsQuery query);
    
    /**
     * Get all loans for a customer
     */
    List<LoanSummary> getCustomerLoans(GetCustomerLoansQuery query);
}

/**
 * Payment Processing Use Case
 */
public interface PaymentProcessingUseCase {
    
    /**
     * Process a loan payment
     */
    PaymentResult processPayment(ProcessPaymentCommand command);
    
    /**
     * Schedule a future payment
     */
    PaymentScheduleResult schedulePayment(SchedulePaymentCommand command);
    
    /**
     * Get payment history for a loan
     */
    List<PaymentHistory> getPaymentHistory(GetPaymentHistoryQuery query);
    
    /**
     * Calculate early payment options
     */
    EarlyPaymentOptions calculateEarlyPayment(CalculateEarlyPaymentQuery query);
}

/**
 * Customer Management Use Case
 */
public interface CustomerManagementUseCase {
    
    /**
     * Create a new customer
     */
    CustomerCreationResult createCustomer(CreateCustomerCommand command);
    
    /**
     * Update customer information
     */
    CustomerUpdateResult updateCustomer(UpdateCustomerCommand command);
    
    /**
     * Get customer details
     */
    CustomerDetails getCustomerDetails(GetCustomerDetailsQuery query);
    
    /**
     * Perform KYC verification
     */
    KYCVerificationResult performKYCVerification(PerformKYCCommand command);
    
    /**
     * Block/unblock customer
     */
    CustomerBlockResult blockCustomer(BlockCustomerCommand command);
}

/**
 * AI Services Use Case
 */
public interface AIServicesUseCase {
    
    /**
     * Perform fraud detection analysis
     */
    FraudDetectionResult performFraudDetection(FraudDetectionCommand command);
    
    /**
     * Generate loan recommendations
     */
    LoanRecommendationResult generateRecommendations(GenerateRecommendationsCommand command);
    
    /**
     * Perform risk assessment
     */
    RiskAssessmentResult performRiskAssessment(RiskAssessmentCommand command);
    
    /**
     * Query RAG system
     */
    RAGQueryResult queryRAG(RAGQueryCommand command);
}

/**
 * Compliance Use Case
 */
public interface ComplianceUseCase {
    
    /**
     * Perform compliance check
     */
    ComplianceCheckResult performComplianceCheck(ComplianceCheckCommand command);
    
    /**
     * Generate regulatory report
     */
    RegulatoryReportResult generateReport(GenerateReportCommand command);
    
    /**
     * Audit transaction
     */
    AuditResult auditTransaction(AuditTransactionCommand command);
}