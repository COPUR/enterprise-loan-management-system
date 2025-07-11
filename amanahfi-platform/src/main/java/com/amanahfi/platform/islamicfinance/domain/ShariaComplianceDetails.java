package com.amanahfi.platform.islamicfinance.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Value object containing Sharia compliance validation details
 * 
 * This object encapsulates all information related to the Sharia
 * compliance status of an Islamic finance product, including
 * validation authority, principles applied, and compliance notes.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
@Builder
public class ShariaComplianceDetails {
    
    /**
     * Whether the product is Sharia compliant
     */
    boolean compliant;
    
    /**
     * The authority that validated compliance
     */
    String validatingAuthority;
    
    /**
     * Date and time of validation
     */
    LocalDateTime validationDate;
    
    /**
     * Reference number or fatwa reference
     */
    String referenceNumber;
    
    /**
     * Islamic principles that apply to this product
     */
    List<String> applicablePrinciples;
    
    /**
     * Detailed compliance notes
     */
    String complianceNotes;
    
    /**
     * Whether the product is free from Riba (interest)
     */
    boolean ribaFree;
    
    /**
     * Whether the product is free from Gharar (uncertainty)
     */
    boolean ghararFree;
    
    /**
     * Whether the product is backed by real assets
     */
    boolean assetBacked;
    
    /**
     * Whether the underlying asset is permissible under Sharia
     */
    boolean permissibleAsset;
    
    /**
     * Whether the contract structure is Sharia compliant
     */
    boolean compliantContractStructure;

    /**
     * Creates a compliant Sharia compliance details object
     */
    public static ShariaComplianceDetails compliant(String authority, String referenceNumber) {
        return ShariaComplianceDetails.builder()
                .compliant(true)
                .validatingAuthority(authority)
                .validationDate(LocalDateTime.now())
                .referenceNumber(referenceNumber)
                .applicablePrinciples(List.of(
                    "No Riba (Interest)",
                    "No Gharar (Uncertainty)",
                    "Asset-backed transaction",
                    "Permissible business activity"
                ))
                .complianceNotes("Product fully complies with Sharia principles")
                .ribaFree(true)
                .ghararFree(true)
                .assetBacked(true)
                .permissibleAsset(true)
                .compliantContractStructure(true)
                .build();
    }

    /**
     * Creates a non-compliant Sharia compliance details object
     */
    public static ShariaComplianceDetails nonCompliant(String authority, String reason) {
        return ShariaComplianceDetails.builder()
                .compliant(false)
                .validatingAuthority(authority)
                .validationDate(LocalDateTime.now())
                .complianceNotes(reason)
                .ribaFree(false)
                .ghararFree(false)
                .assetBacked(false)
                .permissibleAsset(false)
                .compliantContractStructure(false)
                .build();
    }

    /**
     * Validates the overall compliance based on individual criteria
     */
    public boolean isFullyCompliant() {
        return compliant && 
               ribaFree && 
               ghararFree && 
               assetBacked && 
               permissibleAsset && 
               compliantContractStructure;
    }
}