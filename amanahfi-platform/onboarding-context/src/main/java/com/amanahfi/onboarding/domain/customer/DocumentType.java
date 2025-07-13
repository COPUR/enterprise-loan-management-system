package com.amanahfi.onboarding.domain.customer;

/**
 * Document Types for KYC Verification
 * UAE-specific document types required for customer onboarding
 */
public enum DocumentType {
    /**
     * UAE Emirates ID (front side)
     */
    EMIRATES_ID,
    
    /**
     * UAE Emirates ID (back side)
     */
    EMIRATES_ID_BACK,
    
    /**
     * Passport (for expatriates)
     */
    PASSPORT,
    
    /**
     * Visa (for expatriates)
     */
    VISA,
    
    /**
     * Salary certificate
     */
    SALARY_CERTIFICATE,
    
    /**
     * Bank statement
     */
    BANK_STATEMENT,
    
    /**
     * Trade license (for businesses)
     */
    TRADE_LICENSE,
    
    /**
     * Memorandum of Association (for businesses)
     */
    MEMORANDUM_OF_ASSOCIATION,
    
    /**
     * Authorized signatory letter (for businesses)
     */
    AUTHORIZED_SIGNATORY_LETTER,
    
    /**
     * Utility bill for address verification
     */
    UTILITY_BILL,
    
    /**
     * Employment letter
     */
    EMPLOYMENT_LETTER,
    
    /**
     * Other supporting documents
     */
    OTHER
}