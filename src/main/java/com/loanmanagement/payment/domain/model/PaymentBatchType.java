package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different types of payment batches.
 */
public enum PaymentBatchType {
    LOAN_PAYMENTS("Loan Payments"),
    INTEREST_PAYMENTS("Interest Payments"),
    PRINCIPAL_PAYMENTS("Principal Payments"),
    FEE_PAYMENTS("Fee Payments"),
    PENALTY_PAYMENTS("Penalty Payments"),
    ESCROW_PAYMENTS("Escrow Payments"),
    INSURANCE_PAYMENTS("Insurance Payments"),
    TAX_PAYMENTS("Tax Payments"),
    DISBURSEMENTS("Disbursements"),
    REFUNDS("Refunds"),
    REVERSALS("Reversals"),
    CORRECTIONS("Corrections"),
    AUTOMATIC_PAYMENTS("Automatic Payments"),
    SCHEDULED_PAYMENTS("Scheduled Payments"),
    RECURRING_PAYMENTS("Recurring Payments"),
    MANUAL_PAYMENTS("Manual Payments"),
    BULK_PAYMENTS("Bulk Payments"),
    PAYROLL_DEDUCTIONS("Payroll Deductions"),
    WIRE_TRANSFERS("Wire Transfers"),
    ACH_PAYMENTS("ACH Payments");

    private final String displayName;

    PaymentBatchType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAutomated() {
        return this == AUTOMATIC_PAYMENTS || this == SCHEDULED_PAYMENTS || this == RECURRING_PAYMENTS;
    }

    public boolean requiresApproval() {
        return this == DISBURSEMENTS || this == REFUNDS || this == REVERSALS || this == WIRE_TRANSFERS;
    }

    public boolean isLoanRelated() {
        return this == LOAN_PAYMENTS || this == INTEREST_PAYMENTS || this == PRINCIPAL_PAYMENTS || 
               this == FEE_PAYMENTS || this == PENALTY_PAYMENTS || this == ESCROW_PAYMENTS;
    }
}