package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Disbursement Instructions Value Object
 * Contains instructions for how loan funds should be disbursed
 */
@Value
@Builder(toBuilder = true)
public class DisbursementInstructions {
    
    String accountNumber;
    String routingNumber;
    DisbursementMethod disbursementMethod;
    LocalDate disbursementDate;
    String specialInstructions;
    
    // Additional disbursement details
    String accountHolderName;
    String bankName;
    String wireInstructions;
    boolean requiresVerification;
    String verificationMethod;
    
    public DisbursementInstructions(String accountNumber, String routingNumber, DisbursementMethod disbursementMethod,
                                  LocalDate disbursementDate, String specialInstructions, String accountHolderName,
                                  String bankName, String wireInstructions, boolean requiresVerification,
                                  String verificationMethod) {
        
        // Validation
        Objects.requireNonNull(disbursementMethod, "Disbursement method cannot be null");
        Objects.requireNonNull(disbursementDate, "Disbursement date cannot be null");
        
        if (disbursementMethod != DisbursementMethod.CHECK) {
            Objects.requireNonNull(accountNumber, "Account number required for electronic disbursement");
            Objects.requireNonNull(routingNumber, "Routing number required for electronic disbursement");
            
            if (accountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Account number cannot be empty");
            }
            if (routingNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Routing number cannot be empty");
            }
        }
        
        if (disbursementDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Disbursement date cannot be in the past");
        }
        
        this.accountNumber = accountNumber != null ? accountNumber.trim() : null;
        this.routingNumber = routingNumber != null ? routingNumber.trim() : null;
        this.disbursementMethod = disbursementMethod;
        this.disbursementDate = disbursementDate;
        this.specialInstructions = specialInstructions;
        this.accountHolderName = accountHolderName;
        this.bankName = bankName;
        this.wireInstructions = wireInstructions;
        this.requiresVerification = requiresVerification;
        this.verificationMethod = verificationMethod;
    }
    
    /**
     * Create direct deposit instructions
     */
    public static DisbursementInstructions directDeposit(String accountNumber, String routingNumber, 
                                                        LocalDate disbursementDate) {
        return DisbursementInstructions.builder()
                .accountNumber(accountNumber)
                .routingNumber(routingNumber)
                .disbursementMethod(DisbursementMethod.DIRECT_DEPOSIT)
                .disbursementDate(disbursementDate)
                .requiresVerification(true)
                .verificationMethod("MICRO_DEPOSITS")
                .build();
    }
    
    /**
     * Create wire transfer instructions
     */
    public static DisbursementInstructions wireTransfer(String accountNumber, String routingNumber,
                                                       String wireInstructions, LocalDate disbursementDate) {
        return DisbursementInstructions.builder()
                .accountNumber(accountNumber)
                .routingNumber(routingNumber)
                .disbursementMethod(DisbursementMethod.WIRE_TRANSFER)
                .disbursementDate(disbursementDate)
                .wireInstructions(wireInstructions)
                .requiresVerification(true)
                .verificationMethod("BANK_VERIFICATION")
                .build();
    }
    
    /**
     * Create check disbursement instructions
     */
    public static DisbursementInstructions check(LocalDate disbursementDate, String specialInstructions) {
        return DisbursementInstructions.builder()
                .disbursementMethod(DisbursementMethod.CHECK)
                .disbursementDate(disbursementDate)
                .specialInstructions(specialInstructions)
                .requiresVerification(false)
                .build();
    }
    
    /**
     * Check if disbursement requires bank account verification
     */
    public boolean requiresBankVerification() {
        return disbursementMethod != DisbursementMethod.CHECK && requiresVerification;
    }
    
    /**
     * Check if disbursement is scheduled for future
     */
    public boolean isScheduledForFuture() {
        return disbursementDate.isAfter(LocalDate.now());
    }
    
    /**
     * Get masked account number for display
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "****" + lastFour;
    }
    
    /**
     * Validate instructions are complete
     */
    public boolean isComplete() {
        return switch (disbursementMethod) {
            case DIRECT_DEPOSIT, ACH_TRANSFER -> 
                accountNumber != null && !accountNumber.isEmpty() &&
                routingNumber != null && !routingNumber.isEmpty();
            case WIRE_TRANSFER -> 
                accountNumber != null && !accountNumber.isEmpty() &&
                routingNumber != null && !routingNumber.isEmpty() &&
                wireInstructions != null && !wireInstructions.isEmpty();
            case CHECK -> true; // No additional validation needed for checks
        };
    }
}