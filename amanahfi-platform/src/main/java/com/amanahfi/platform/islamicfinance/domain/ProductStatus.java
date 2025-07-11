package com.amanahfi.platform.islamicfinance.domain;

/**
 * Enumeration of Islamic Finance Product statuses
 * 
 * Represents the lifecycle stages of an Islamic finance product
 * from initial creation through completion.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
public enum ProductStatus {
    
    /**
     * Product is in draft state, not yet submitted for approval
     */
    DRAFT("Draft", "Product is being configured"),
    
    /**
     * Product has been submitted for Sharia compliance review
     */
    PENDING_SHARIA_REVIEW("Pending Sharia Review", "Awaiting Higher Sharia Authority approval"),
    
    /**
     * Product has been approved by Sharia board and risk assessment
     */
    APPROVED("Approved", "Product approved for activation"),
    
    /**
     * Product is active and available for customers
     */
    ACTIVE("Active", "Product is available for new applications"),
    
    /**
     * Product is suspended temporarily
     */
    SUSPENDED("Suspended", "Product temporarily unavailable"),
    
    /**
     * Product has been disbursed to customer
     */
    DISBURSED("Disbursed", "Funds have been disbursed to customer"),
    
    /**
     * Product is in repayment phase
     */
    IN_REPAYMENT("In Repayment", "Customer is making payments"),
    
    /**
     * Product has been fully paid and completed
     */
    COMPLETED("Completed", "Product fully paid and closed"),
    
    /**
     * Product has defaulted
     */
    DEFAULTED("Defaulted", "Product in default status"),
    
    /**
     * Product has been cancelled
     */
    CANCELLED("Cancelled", "Product cancelled before activation");

    private final String displayName;
    private final String description;

    ProductStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if the product can be activated
     */
    public boolean canBeActivated() {
        return this == APPROVED;
    }

    /**
     * Checks if the product can be modified
     */
    public boolean canBeModified() {
        return this == DRAFT || this == PENDING_SHARIA_REVIEW;
    }

    /**
     * Checks if the product is in an active state
     */
    public boolean isActive() {
        return this == ACTIVE || this == DISBURSED || this == IN_REPAYMENT;
    }

    /**
     * Checks if the product is in a final state
     */
    public boolean isFinal() {
        return this == COMPLETED || this == DEFAULTED || this == CANCELLED;
    }
}