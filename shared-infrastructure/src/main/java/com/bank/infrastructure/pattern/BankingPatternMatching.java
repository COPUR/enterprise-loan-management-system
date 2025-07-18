package com.bank.infrastructure.pattern;

import com.bank.infrastructure.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Banking Pattern Matching Utilities for Java 21
 * 
 * Leverages Java 21 pattern matching features for:
 * - Transaction type classification
 * - Risk assessment patterns
 * - Compliance rule matching
 * - Islamic banking validation
 * - Fraud detection patterns
 * 
 * Uses record patterns, sealed classes, and switch expressions
 * for clean, readable, and maintainable banking logic.
 */
@Component
public class BankingPatternMatching {

    /**
     * Banking transaction types using sealed classes
     */
    public sealed interface BankingTransaction
        permits RegularTransfer, LoanPayment, IslamicTransaction, FraudulentTransaction {
    }

    /**
     * Regular banking transfer
     */
    public record RegularTransfer(
        String transactionId,
        Money amount,
        LocalDateTime timestamp,
        String customerId,
        String fromAccount,
        String toAccount,
        TransferType type
    ) implements BankingTransaction {
        
        public enum TransferType {
            INTERNAL, EXTERNAL, INTERNATIONAL
        }
    }

    /**
     * Loan payment transaction
     */
    public record LoanPayment(
        String transactionId,
        Money amount,
        LocalDateTime timestamp,
        String customerId,
        String loanId,
        PaymentType paymentType,
        int installmentNumber
    ) implements BankingTransaction {
        
        public enum PaymentType {
            REGULAR, EARLY, PARTIAL, FULL_SETTLEMENT
        }
    }

    /**
     * Islamic banking transaction (Sharia-compliant)
     */
    public record IslamicTransaction(
        String transactionId,
        Money amount,
        LocalDateTime timestamp,
        String customerId,
        IslamicProductType productType,
        String contractId,
        boolean shariaCompliant
    ) implements BankingTransaction {
        
        public enum IslamicProductType {
            MURABAHA, IJARA, MUSHARAKA, MUDARABA, SUKUK
        }
    }

    /**
     * Fraudulent transaction
     */
    public record FraudulentTransaction(
        String transactionId,
        Money amount,
        LocalDateTime timestamp,
        String customerId,
        FraudType fraudType,
        String alertId,
        int riskScore
    ) implements BankingTransaction {
        
        public enum FraudType {
            CARD_SKIMMING, IDENTITY_THEFT, ACCOUNT_TAKEOVER, SYNTHETIC_IDENTITY
        }
    }

    /**
     * Risk assessment using pattern matching
     */
    public RiskLevel assessTransactionRisk(BankingTransaction transaction) {
        return switch (transaction) {
            case RegularTransfer(var id, var amount, var timestamp, var customerId, 
                               var from, var to, var type) -> {
                // Pattern match on transfer type and amount
                yield switch (type) {
                    case INTERNAL -> assessInternalTransferRisk(amount);
                    case EXTERNAL -> assessExternalTransferRisk(amount, timestamp);
                    case INTERNATIONAL -> assessInternationalTransferRisk(amount, customerId);
                };
            }
            
            case LoanPayment(var id, var amount, var timestamp, var customerId,
                           var loanId, var paymentType, var installmentNumber) -> {
                // Pattern match on payment type
                yield switch (paymentType) {
                    case REGULAR -> RiskLevel.LOW;
                    case EARLY -> RiskLevel.MEDIUM;
                    case PARTIAL -> RiskLevel.MEDIUM;
                    case FULL_SETTLEMENT -> RiskLevel.HIGH;
                };
            }
            
            case IslamicTransaction(var id, var amount, var timestamp, var customerId,
                                  var productType, var contractId, var shariaCompliant) -> {
                // Sharia compliance validation
                if (!shariaCompliant) {
                    yield RiskLevel.CRITICAL;
                }
                yield assessIslamicTransactionRisk(productType, amount);
            }
            
            case FraudulentTransaction(var id, var amount, var timestamp, var customerId,
                                     var fraudType, var alertId, var riskScore) -> {
                // Fraud detected - always critical
                yield RiskLevel.CRITICAL;
            }
        };
    }

    /**
     * Transaction processing using pattern matching
     */
    public ProcessingResult processTransaction(BankingTransaction transaction) {
        return switch (transaction) {
            // Pattern matching with guards
            case RegularTransfer t when t.amount().getAmount().compareTo(BigDecimal.valueOf(100000)) > 0 -> {
                yield ProcessingResult.requiresManualApproval(
                    "High value transfer requires manual approval"
                );
            }
            
            case RegularTransfer t when t.type() == RegularTransfer.TransferType.INTERNATIONAL -> {
                yield ProcessingResult.requiresCompliance(
                    "International transfer requires compliance check"
                );
            }
            
            case LoanPayment(var id, var amount, var timestamp, var customerId,
                           var loanId, var paymentType, var installmentNumber) 
                when paymentType == LoanPayment.PaymentType.FULL_SETTLEMENT -> {
                yield ProcessingResult.requiresLoanClosure(
                    "Full settlement requires loan closure processing"
                );
            }
            
            case IslamicTransaction t when !t.shariaCompliant() -> {
                yield ProcessingResult.rejected(
                    "Transaction violates Sharia compliance rules"
                );
            }
            
            case FraudulentTransaction t -> {
                yield ProcessingResult.blocked(
                    "Transaction blocked due to fraud detection: " + t.fraudType()
                );
            }
            
            // Default processing
            default -> ProcessingResult.approved("Transaction approved for processing");
        };
    }

    /**
     * Compliance checking using pattern matching
     */
    public ComplianceResult checkCompliance(BankingTransaction transaction) {
        return switch (transaction) {
            case RegularTransfer(var id, var amount, var timestamp, var customerId,
                               var from, var to, var type) -> {
                // AML compliance for large transfers
                if (amount.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
                    yield ComplianceResult.requiresAMLCheck("Large transfer requires AML verification");
                }
                
                // International transfer compliance
                if (type == RegularTransfer.TransferType.INTERNATIONAL) {
                    yield ComplianceResult.requiresKYC("International transfer requires KYC verification");
                }
                
                yield ComplianceResult.compliant("Transfer meets compliance requirements");
            }
            
            case LoanPayment payment -> {
                // Loan payment compliance
                yield ComplianceResult.compliant("Loan payment compliant");
            }
            
            case IslamicTransaction(var id, var amount, var timestamp, var customerId,
                                  var productType, var contractId, var shariaCompliant) -> {
                // Sharia compliance validation
                if (!shariaCompliant) {
                    yield ComplianceResult.nonCompliant("Transaction violates Sharia principles");
                }
                
                // Product-specific compliance
                yield switch (productType) {
                    case MURABAHA -> validateMurabahaCompliance(amount, contractId);
                    case IJARA -> validateIjaraCompliance(amount, contractId);
                    case MUSHARAKA -> validateMushrakaCompliance(amount, contractId);
                    case MUDARABA -> validateMudarabaCompliance(amount, contractId);
                    case SUKUK -> validateSukukCompliance(amount, contractId);
                };
            }
            
            case FraudulentTransaction fraud -> {
                yield ComplianceResult.nonCompliant("Fraudulent transaction detected");
            }
        };
    }

    /**
     * Transaction categorization using pattern matching
     */
    public TransactionCategory categorizeTransaction(BankingTransaction transaction) {
        return switch (transaction) {
            case RegularTransfer(var id, var amount, var timestamp, var customerId,
                               var from, var to, var type) -> {
                yield switch (type) {
                    case INTERNAL -> TransactionCategory.INTERNAL_TRANSFER;
                    case EXTERNAL -> TransactionCategory.EXTERNAL_TRANSFER;
                    case INTERNATIONAL -> TransactionCategory.INTERNATIONAL_TRANSFER;
                };
            }
            
            case LoanPayment(var id, var amount, var timestamp, var customerId,
                           var loanId, var paymentType, var installmentNumber) -> {
                yield TransactionCategory.LOAN_PAYMENT;
            }
            
            case IslamicTransaction(var id, var amount, var timestamp, var customerId,
                                  var productType, var contractId, var shariaCompliant) -> {
                yield TransactionCategory.ISLAMIC_BANKING;
            }
            
            case FraudulentTransaction(var id, var amount, var timestamp, var customerId,
                                     var fraudType, var alertId, var riskScore) -> {
                yield TransactionCategory.FRAUD_ALERT;
            }
        };
    }

    // Helper methods for risk assessment
    private RiskLevel assessInternalTransferRisk(Money amount) {
        return amount.getAmount().compareTo(BigDecimal.valueOf(50000)) > 0 ? 
            RiskLevel.MEDIUM : RiskLevel.LOW;
    }

    private RiskLevel assessExternalTransferRisk(Money amount, LocalDateTime timestamp) {
        // Higher risk for external transfers, especially outside business hours
        boolean outsideBusinessHours = timestamp.getHour() < 9 || timestamp.getHour() > 17;
        
        if (amount.getAmount().compareTo(BigDecimal.valueOf(25000)) > 0) {
            return outsideBusinessHours ? RiskLevel.HIGH : RiskLevel.MEDIUM;
        }
        
        return outsideBusinessHours ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }

    private RiskLevel assessInternationalTransferRisk(Money amount, String customerId) {
        // International transfers are inherently higher risk
        return amount.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0 ? 
            RiskLevel.HIGH : RiskLevel.MEDIUM;
    }

    private RiskLevel assessIslamicTransactionRisk(IslamicTransaction.IslamicProductType productType, Money amount) {
        return switch (productType) {
            case MURABAHA, IJARA -> RiskLevel.LOW;
            case MUSHARAKA, MUDARABA -> RiskLevel.MEDIUM;
            case SUKUK -> amount.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0 ? 
                RiskLevel.HIGH : RiskLevel.MEDIUM;
        };
    }

    // Helper methods for Islamic banking compliance
    private ComplianceResult validateMurabahaCompliance(Money amount, String contractId) {
        return ComplianceResult.compliant("Murabaha transaction compliant");
    }

    private ComplianceResult validateIjaraCompliance(Money amount, String contractId) {
        return ComplianceResult.compliant("Ijara transaction compliant");
    }

    private ComplianceResult validateMushrakaCompliance(Money amount, String contractId) {
        return ComplianceResult.compliant("Musharaka transaction compliant");
    }

    private ComplianceResult validateMudarabaCompliance(Money amount, String contractId) {
        return ComplianceResult.compliant("Mudaraba transaction compliant");
    }

    private ComplianceResult validateSukukCompliance(Money amount, String contractId) {
        return ComplianceResult.compliant("Sukuk transaction compliant");
    }

    // Enums and result classes
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum TransactionCategory {
        INTERNAL_TRANSFER, EXTERNAL_TRANSFER, INTERNATIONAL_TRANSFER,
        LOAN_PAYMENT, ISLAMIC_BANKING, FRAUD_ALERT
    }

    public sealed interface ProcessingResult
        permits ProcessingResult.Approved, ProcessingResult.RequiresManualApproval, 
                ProcessingResult.RequiresCompliance, ProcessingResult.RequiresLoanClosure,
                ProcessingResult.Rejected, ProcessingResult.Blocked {
        
        String getMessage();
        
        record Approved(String message) implements ProcessingResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record RequiresManualApproval(String message) implements ProcessingResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record RequiresCompliance(String message) implements ProcessingResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record RequiresLoanClosure(String message) implements ProcessingResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record Rejected(String message) implements ProcessingResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record Blocked(String message) implements ProcessingResult {
            @Override
            public String getMessage() { return message; }
        }
        
        static ProcessingResult approved(String message) {
            return new Approved(message);
        }
        
        static ProcessingResult requiresManualApproval(String message) {
            return new RequiresManualApproval(message);
        }
        
        static ProcessingResult requiresCompliance(String message) {
            return new RequiresCompliance(message);
        }
        
        static ProcessingResult requiresLoanClosure(String message) {
            return new RequiresLoanClosure(message);
        }
        
        static ProcessingResult rejected(String message) {
            return new Rejected(message);
        }
        
        static ProcessingResult blocked(String message) {
            return new Blocked(message);
        }
    }

    public sealed interface ComplianceResult
        permits ComplianceResult.Compliant, ComplianceResult.RequiresAMLCheck, 
                ComplianceResult.RequiresKYC, ComplianceResult.NonCompliant {
        
        String getMessage();
        
        record Compliant(String message) implements ComplianceResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record RequiresAMLCheck(String message) implements ComplianceResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record RequiresKYC(String message) implements ComplianceResult {
            @Override
            public String getMessage() { return message; }
        }
        
        record NonCompliant(String message) implements ComplianceResult {
            @Override
            public String getMessage() { return message; }
        }
        
        static ComplianceResult compliant(String message) {
            return new Compliant(message);
        }
        
        static ComplianceResult requiresAMLCheck(String message) {
            return new RequiresAMLCheck(message);
        }
        
        static ComplianceResult requiresKYC(String message) {
            return new RequiresKYC(message);
        }
        
        static ComplianceResult nonCompliant(String message) {
            return new NonCompliant(message);
        }
    }
}