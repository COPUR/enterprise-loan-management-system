package com.bank.payment.infrastructure.external;

import com.bank.payment.application.ComplianceService;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Adapter for external compliance validation service
 * 
 * Implements Hexagonal Architecture - Infrastructure adapter for compliance
 * Handles regulatory requirements (AML, KYC, BSA, etc.)
 */
@Component
public class ComplianceServiceAdapter implements ComplianceService {
    
    private static final BigDecimal KYC_THRESHOLD = BigDecimal.valueOf(3000);
    private static final BigDecimal REPORTING_THRESHOLD = BigDecimal.valueOf(10000);
    private static final BigDecimal CTR_THRESHOLD = BigDecimal.valueOf(10000); // Currency Transaction Report
    
    @Override
    public boolean validatePayment(Payment payment) {
        try {
            // AML (Anti-Money Laundering) checks
            if (exceedsReportingThreshold(payment)) {
                // Large transactions require additional scrutiny
                if (!performEnhancedDueDiligence(payment)) {
                    return false;
                }
            }
            
            // KYC (Know Your Customer) validation
            if (requiresKycVerification(payment)) {
                if (!isKycCompliant(payment)) {
                    return false;
                }
            }
            
            // Sanctions screening
            if (involvesSanctionedEntities(payment)) {
                return false; // Block payments to/from sanctioned entities
            }
            
            // BSA (Bank Secrecy Act) compliance
            if (requiresBsaReporting(payment)) {
                generateComplianceReport(payment);
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Compliance service error: " + e.getMessage());
            // Conservative approach - fail closed for compliance
            return false;
        }
    }
    
    @Override
    public boolean requiresKycVerification(Payment payment) {
        BigDecimal amount = payment.getAmount().getAmount();
        
        // KYC required for payments above threshold
        if (amount.compareTo(KYC_THRESHOLD) > 0) {
            return true;
        }
        
        // Always require KYC for wire transfers
        if (payment.getPaymentType() == PaymentType.WIRE_TRANSFER) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean exceedsReportingThreshold(Payment payment) {
        BigDecimal amount = payment.getAmount().getAmount();
        return amount.compareTo(REPORTING_THRESHOLD) > 0;
    }
    
    @Override
    public boolean involvesSanctionedEntities(Payment payment) {
        // Simplified sanctions screening
        // In practice, this would check against OFAC lists, UN sanctions, etc.
        
        String fromAccount = payment.getFromAccountId().getValue();
        String toAccount = payment.getToAccountId().getValue();
        
        // Mock sanctions check - in practice would query external services
        return fromAccount.contains("BLOCKED") || toAccount.contains("BLOCKED");
    }
    
    @Override
    public String generateComplianceReport(Payment payment) {
        StringBuilder report = new StringBuilder();
        
        report.append("COMPLIANCE REPORT\n");
        report.append("=================\n");
        report.append("Payment ID: ").append(payment.getId().getValue()).append("\n");
        report.append("Customer ID: ").append(payment.getCustomerId().getValue()).append("\n");
        report.append("Amount: ").append(payment.getAmount()).append("\n");
        report.append("Payment Type: ").append(payment.getPaymentType()).append("\n");
        report.append("From Account: ").append(payment.getFromAccountId().getValue()).append("\n");
        report.append("To Account: ").append(payment.getToAccountId().getValue()).append("\n");
        report.append("Timestamp: ").append(payment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        
        // Add compliance flags
        if (exceedsReportingThreshold(payment)) {
            report.append("FLAG: Exceeds reporting threshold\n");
        }
        
        if (requiresKycVerification(payment)) {
            report.append("FLAG: Requires KYC verification\n");
        }
        
        if (payment.getPaymentType() == PaymentType.WIRE_TRANSFER) {
            report.append("FLAG: Wire transfer - enhanced monitoring\n");
        }
        
        String reportContent = report.toString();
        
        // In practice, this would be sent to compliance systems
        System.out.println("Generated compliance report for payment: " + payment.getId().getValue());
        
        return reportContent;
    }
    
    /**
     * Enhanced Due Diligence for high-value transactions
     */
    private boolean performEnhancedDueDiligence(Payment payment) {
        // Simplified EDD process
        // In practice, this would involve:
        // - Source of funds verification
        // - Beneficial ownership identification
        // - Purpose of transaction verification
        // - Enhanced customer screening
        
        BigDecimal amount = payment.getAmount().getAmount();
        
        // For very large amounts, require additional verification
        if (amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            // Would integrate with external EDD systems
            return false; // Conservative approach for demo
        }
        
        return true;
    }
    
    /**
     * Check if customer is KYC compliant
     */
    private boolean isKycCompliant(Payment payment) {
        // Simplified KYC check
        // In practice, this would check customer KYC status in CRM systems
        
        String customerId = payment.getCustomerId().getValue();
        
        // Mock implementation - assume customers are KYC compliant
        // unless they have specific indicators
        return !customerId.contains("PENDING_KYC");
    }
    
    /**
     * Check if transaction requires BSA reporting
     */
    private boolean requiresBsaReporting(Payment payment) {
        return exceedsReportingThreshold(payment) || 
               payment.getPaymentType() == PaymentType.WIRE_TRANSFER;
    }
}