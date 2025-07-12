package com.amanahfi.platform.shared.exception;

import com.amanahfi.platform.shared.i18n.MessageService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Map;

/**
 * Exception for business rule violations
 */
@Getter
@Configurable
public class BusinessException extends AmanahFiException {
    
    @Autowired
    private transient MessageService messageService;
    
    private final String businessRule;
    private final Object[] messageParams;
    
    public BusinessException(
            String errorCode,
            String businessRule,
            String userMessage,
            String technicalMessage,
            ErrorSeverity severity,
            Map<String, Object> errorContext,
            Object... messageParams) {
        
        super(errorCode, userMessage, technicalMessage, severity, ErrorCategory.BUSINESS, errorContext);
        this.businessRule = businessRule;
        this.messageParams = messageParams != null ? messageParams.clone() : new Object[0];
    }
    
    public BusinessException(
            String errorCode,
            String businessRule,
            String userMessage,
            String technicalMessage,
            Map<String, Object> errorContext,
            Object... messageParams) {
        
        this(errorCode, businessRule, userMessage, technicalMessage, ErrorSeverity.MEDIUM, errorContext, messageParams);
    }
    
    public BusinessException(
            String errorCode,
            String businessRule,
            String userMessage,
            String technicalMessage,
            Object... messageParams) {
        
        this(errorCode, businessRule, userMessage, technicalMessage, Map.of(), messageParams);
    }
    
    @Override
    public String getLocalizedMessage(String languageCode) {
        if (messageService == null) {
            return getUserMessage(); // Fallback if DI not available
        }
        
        try {
            // Try to get localized message using error code as key
            if (messageService.hasMessage(getErrorCode(), languageCode)) {
                return messageService.getMessage(getErrorCode(), languageCode, messageParams);
            }
            
            // Try with business rule as key
            if (messageService.hasMessage(businessRule, languageCode)) {
                return messageService.getMessage(businessRule, languageCode, messageParams);
            }
            
            // Fallback to original user message
            return getUserMessage();
            
        } catch (Exception e) {
            return getUserMessage(); // Fallback on any error
        }
    }
    
    // Common business exceptions factory methods
    
    public static BusinessException invalidLoanAmount(double amount, double minAmount, double maxAmount) {
        return new BusinessException(
            "LOAN_AMOUNT_INVALID",
            "loan.amount.validation",
            "Loan amount must be between " + minAmount + " and " + maxAmount,
            "Invalid loan amount: " + amount + ", allowed range: " + minAmount + "-" + maxAmount,
            Map.of(
                "amount", amount,
                "minAmount", minAmount,
                "maxAmount", maxAmount
            ),
            amount, minAmount, maxAmount
        );
    }
    
    public static BusinessException customerNotEligible(String customerId, String reason) {
        return new BusinessException(
            "CUSTOMER_NOT_ELIGIBLE",
            "customer.eligibility.failed",
            "Customer is not eligible for this product",
            "Customer " + customerId + " is not eligible: " + reason,
            ErrorSeverity.HIGH,
            Map.of(
                "customerId", customerId,
                "reason", reason
            ),
            customerId, reason
        );
    }
    
    public static BusinessException islamicFinanceViolation(String violationType, String description) {
        return new BusinessException(
            "ISLAMIC_FINANCE_VIOLATION",
            "islamic.finance.violation",
            "This operation violates Islamic finance principles",
            "Islamic finance violation: " + violationType + " - " + description,
            ErrorSeverity.HIGH,
            Map.of(
                "violationType", violationType,
                "description", description,
                "category", "SHARIA_COMPLIANCE"
            ),
            violationType, description
        );
    }
    
    public static BusinessException insufficientFunds(String accountId, double requested, double available) {
        return new BusinessException(
            "INSUFFICIENT_FUNDS",
            "account.insufficient.funds",
            "Insufficient funds in account",
            "Account " + accountId + " has insufficient funds. Requested: " + requested + ", Available: " + available,
            ErrorSeverity.MEDIUM,
            Map.of(
                "accountId", accountId,
                "requestedAmount", requested,
                "availableAmount", available
            ),
            accountId, requested, available
        );
    }
    
    public static BusinessException loanAlreadyExists(String customerId, String loanType) {
        return new BusinessException(
            "LOAN_ALREADY_EXISTS",
            "loan.already.exists",
            "Customer already has an active loan of this type",
            "Customer " + customerId + " already has an active " + loanType + " loan",
            Map.of(
                "customerId", customerId,
                "loanType", loanType
            ),
            customerId, loanType
        );
    }
    
    public static BusinessException invalidPaymentSchedule(String reason) {
        return new BusinessException(
            "INVALID_PAYMENT_SCHEDULE",
            "payment.schedule.invalid",
            "Invalid payment schedule configuration",
            "Invalid payment schedule: " + reason,
            Map.of("reason", reason),
            reason
        );
    }
    
    public static BusinessException cbdcTransactionLimitExceeded(double amount, double limit) {
        return new BusinessException(
            "CBDC_TRANSACTION_LIMIT_EXCEEDED",
            "cbdc.transaction.limit.exceeded",
            "CBDC transaction amount exceeds daily limit",
            "CBDC transaction amount " + amount + " exceeds daily limit of " + limit,
            ErrorSeverity.HIGH,
            Map.of(
                "amount", amount,
                "limit", limit,
                "category", "CBDC_COMPLIANCE"
            ),
            amount, limit
        );
    }
    
    public static BusinessException regulatoryComplianceViolation(String regulation, String requirement, String violation) {
        return new BusinessException(
            "REGULATORY_COMPLIANCE_VIOLATION",
            "regulatory.compliance.violation",
            "Operation violates regulatory requirements",
            "Violation of " + regulation + " requirement '" + requirement + "': " + violation,
            ErrorSeverity.CRITICAL,
            Map.of(
                "regulation", regulation,
                "requirement", requirement,
                "violation", violation,
                "category", "REGULATORY_COMPLIANCE"
            ),
            regulation, requirement, violation
        );
    }
}