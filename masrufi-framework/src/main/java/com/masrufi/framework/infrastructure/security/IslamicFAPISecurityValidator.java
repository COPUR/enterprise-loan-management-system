package com.masrufi.framework.infrastructure.security;

// Local stubs — replace com.bank.infrastructure.security.{FAPISecurityValidator,FAPISecurityException}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Islamic FAPI Security Validator for MasruFi Framework
 * 
 * Extends the enterprise FAPISecurityValidator to provide Islamic
 * finance-specific
 * security validations including:
 * - Sharia compliance validation for financial transactions
 * - Halal asset verification in transaction requests
 * - Riba (interest) detection in financial contracts
 * - Gharar (uncertainty) validation in transaction parameters
 * - Islamic finance transaction signing and verification
 * - UAE Islamic banking regulatory compliance
 * 
 * Islamic Finance Security Requirements:
 * - All transactions must be Sharia compliant
 * - Asset backing must be verified and Halal
 * - Profit margins must be within acceptable Islamic limits
 * - No interest-based calculations or references
 * - Gharar must be minimized through clear specifications
 * 
 * UAE Islamic Banking Compliance:
 * - UAE Central Bank Islamic banking regulations
 * - UAE Higher Sharia Authority standards
 * - AAOIFI (Accounting and Auditing Organization for Islamic Financial
 * Institutions) compliance
 */
@Component
public class IslamicFAPISecurityValidator extends FAPISecurityValidatorBase {

    private static final Logger logger = LoggerFactory.getLogger(IslamicFAPISecurityValidator.class);

    // Islamic finance validation patterns
    private static final Pattern RIBA_KEYWORDS = Pattern.compile(
            "(?i)\\b(interest|apr|apy|yield|rate|usury|riba)\\b");

    private static final Pattern GHARAR_KEYWORDS = Pattern.compile(
            "(?i)\\b(gambling|speculation|uncertainty|derivative|option|future)\\b");

    private static final Pattern HARAM_ASSETS = Pattern.compile(
            "(?i)\\b(alcohol|pork|tobacco|gambling|casino|lottery|insurance|conventional_bank)\\b");

    // Islamic finance limits
    private static final double MAX_PROFIT_MARGIN = 0.30; // 30% maximum profit margin
    private static final double MIN_ASSET_BACKING_RATIO = 0.95; // 95% minimum asset backing

    // UAE Islamic banking requirements
    private static final String UAE_ISLAMIC_AUTHORITY = "UAE_HIGHER_SHARIA_AUTHORITY";
    private static final String AAOIFI_COMPLIANCE = "AAOIFI_STANDARD";

    public IslamicFAPISecurityValidator(String signingKey) {
        super(signingKey);
    }

    /**
     * Validate Islamic finance transaction for Sharia compliance
     * 
     * @param transaction     the transaction to validate
     * @param transactionType the type of Islamic finance transaction
     * @throws FAPISecurityException if validation fails
     */
    public void validateIslamicTransaction(Object transaction, String transactionType) {
        logger.debug("Validating Islamic transaction: {}", transactionType);

        try {
            // Basic FAPI validation first
            validateRequestSignature(transaction, generateHMACSignature(transaction));

            // Islamic finance specific validations
            validateShariaCompliance(transaction);
            validateRibaFree(transaction);
            validateGhararFree(transaction);
            validateHalalAssets(transaction);

            // Transaction type specific validations
            switch (transactionType.toLowerCase()) {
                case "murabaha":
                    validateMurabahaTransaction(transaction);
                    break;
                case "musharakah":
                    validateMusharakahTransaction(transaction);
                    break;
                case "ijarah":
                    validateIjarahTransaction(transaction);
                    break;
                case "salam":
                    validateSalamTransaction(transaction);
                    break;
                case "istisna":
                    validateIstisnaTransaction(transaction);
                    break;
                case "qard_hassan":
                    validateQardHassanTransaction(transaction);
                    break;
                default:
                    throw new FAPISecurityException("Unsupported Islamic finance transaction type: " + transactionType,
                            "invalid_transaction_type");
            }

            logger.debug("Islamic transaction validation successful for: {}", transactionType);

        } catch (Exception e) {
            logger.error("Islamic transaction validation failed", e);
            if (e instanceof FAPISecurityException) {
                throw e;
            }
            throw new FAPISecurityException("Islamic transaction validation failed: " + e.getMessage(),
                    "islamic_validation_error", e);
        }
    }

    /**
     * Validate overall Sharia compliance of transaction
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if not Sharia compliant
     */
    private void validateShariaCompliance(Object transaction) {
        String transactionJson = convertToJson(transaction);

        // Check for Sharia compliance indicators
        Map<String, Object> transactionData = parseTransactionData(transactionJson);

        Boolean shariaCompliant = (Boolean) transactionData.get("shariaCompliant");
        if (shariaCompliant != null && !shariaCompliant) {
            throw new FAPISecurityException("Transaction is not Sharia compliant", "sharia_violation");
        }

        String complianceAuthority = (String) transactionData.get("complianceAuthority");
        if (complianceAuthority != null && !UAE_ISLAMIC_AUTHORITY.equals(complianceAuthority)) {
            logger.warn("Transaction not validated by UAE Higher Sharia Authority: {}", complianceAuthority);
        }
    }

    /**
     * Validate transaction is free from Riba (interest)
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if Riba detected
     */
    private void validateRibaFree(Object transaction) {
        String transactionJson = convertToJson(transaction);

        if (RIBA_KEYWORDS.matcher(transactionJson).find()) {
            throw new FAPISecurityException("Riba (interest) detected in transaction", "riba_violation");
        }

        // Check for numeric values that might indicate interest rates
        Map<String, Object> transactionData = parseTransactionData(transactionJson);

        Double interestRate = (Double) transactionData.get("interestRate");
        if (interestRate != null && interestRate > 0) {
            throw new FAPISecurityException("Interest rate detected in Islamic transaction", "riba_violation");
        }

        Double apr = (Double) transactionData.get("apr");
        if (apr != null && apr > 0) {
            throw new FAPISecurityException("APR detected in Islamic transaction", "riba_violation");
        }
    }

    /**
     * Validate transaction is free from Gharar (uncertainty)
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if Gharar detected
     */
    private void validateGhararFree(Object transaction) {
        String transactionJson = convertToJson(transaction);

        if (GHARAR_KEYWORDS.matcher(transactionJson).find()) {
            throw new FAPISecurityException("Gharar (uncertainty) detected in transaction", "gharar_violation");
        }

        // Check for required specifications to minimize Gharar
        Map<String, Object> transactionData = parseTransactionData(transactionJson);

        String assetDescription = (String) transactionData.get("assetDescription");
        if (assetDescription == null || assetDescription.trim().isEmpty()) {
            throw new FAPISecurityException("Asset description required to minimize Gharar", "gharar_violation");
        }

        String deliveryDate = (String) transactionData.get("deliveryDate");
        if (deliveryDate == null || deliveryDate.trim().isEmpty()) {
            throw new FAPISecurityException("Delivery date required to minimize Gharar", "gharar_violation");
        }
    }

    /**
     * Validate transaction involves only Halal assets
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if Haram assets detected
     */
    private void validateHalalAssets(Object transaction) {
        String transactionJson = convertToJson(transaction);

        if (HARAM_ASSETS.matcher(transactionJson).find()) {
            throw new FAPISecurityException("Haram (forbidden) assets detected in transaction",
                    "haram_asset_violation");
        }

        // Check asset permissibility flag
        Map<String, Object> transactionData = parseTransactionData(transactionJson);

        Boolean assetPermissible = (Boolean) transactionData.get("assetPermissible");
        if (assetPermissible != null && !assetPermissible) {
            throw new FAPISecurityException("Asset is not permissible in Islamic finance", "haram_asset_violation");
        }
    }

    /**
     * Validate Murabaha transaction specific requirements
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if validation fails
     */
    private void validateMurabahaTransaction(Object transaction) {
        Map<String, Object> transactionData = parseTransactionData(convertToJson(transaction));

        // Validate profit margin
        Double profitMargin = (Double) transactionData.get("profitMargin");
        if (profitMargin == null) {
            throw new FAPISecurityException("Profit margin required for Murabaha transaction",
                    "murabaha_validation_error");
        }

        if (profitMargin > MAX_PROFIT_MARGIN) {
            throw new FAPISecurityException("Profit margin exceeds maximum allowed: " + MAX_PROFIT_MARGIN,
                    "excessive_profit_margin");
        }

        // Validate asset ownership
        Boolean assetOwned = (Boolean) transactionData.get("assetOwned");
        if (assetOwned == null || !assetOwned) {
            throw new FAPISecurityException("Asset must be owned by seller in Murabaha transaction",
                    "murabaha_validation_error");
        }

        // Validate cost disclosure
        Double assetCost = (Double) transactionData.get("assetCost");
        if (assetCost == null) {
            throw new FAPISecurityException("Asset cost must be disclosed in Murabaha transaction",
                    "murabaha_validation_error");
        }
    }

    /**
     * Validate Musharakah transaction specific requirements
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if validation fails
     */
    private void validateMusharakahTransaction(Object transaction) {
        Map<String, Object> transactionData = parseTransactionData(convertToJson(transaction));

        // Validate profit/loss sharing arrangement
        String profitSharingRatio = (String) transactionData.get("profitSharingRatio");
        if (profitSharingRatio == null || profitSharingRatio.trim().isEmpty()) {
            throw new FAPISecurityException("Profit sharing ratio required for Musharakah transaction",
                    "musharakah_validation_error");
        }

        String lossSharingRatio = (String) transactionData.get("lossSharingRatio");
        if (lossSharingRatio == null || lossSharingRatio.trim().isEmpty()) {
            throw new FAPISecurityException("Loss sharing ratio required for Musharakah transaction",
                    "musharakah_validation_error");
        }

        // Validate capital contributions
        Double capitalContribution = (Double) transactionData.get("capitalContribution");
        if (capitalContribution == null || capitalContribution <= 0) {
            throw new FAPISecurityException("Valid capital contribution required for Musharakah transaction",
                    "musharakah_validation_error");
        }
    }

    /**
     * Validate Ijarah transaction specific requirements
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if validation fails
     */
    private void validateIjarahTransaction(Object transaction) {
        Map<String, Object> transactionData = parseTransactionData(convertToJson(transaction));

        // Validate lease terms
        String leaseTerm = (String) transactionData.get("leaseTerm");
        if (leaseTerm == null || leaseTerm.trim().isEmpty()) {
            throw new FAPISecurityException("Lease term required for Ijarah transaction",
                    "ijarah_validation_error");
        }

        Double rentalAmount = (Double) transactionData.get("rentalAmount");
        if (rentalAmount == null || rentalAmount <= 0) {
            throw new FAPISecurityException("Valid rental amount required for Ijarah transaction",
                    "ijarah_validation_error");
        }

        // Validate asset ownership
        Boolean assetOwned = (Boolean) transactionData.get("assetOwned");
        if (assetOwned == null || !assetOwned) {
            throw new FAPISecurityException("Asset must be owned by lessor in Ijarah transaction",
                    "ijarah_validation_error");
        }
    }

    /**
     * Validate Salam transaction specific requirements
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if validation fails
     */
    private void validateSalamTransaction(Object transaction) {
        Map<String, Object> transactionData = parseTransactionData(convertToJson(transaction));

        // Validate advance payment
        Boolean advancePayment = (Boolean) transactionData.get("advancePayment");
        if (advancePayment == null || !advancePayment) {
            throw new FAPISecurityException("Advance payment required for Salam transaction",
                    "salam_validation_error");
        }

        // Validate commodity specifications
        String commoditySpec = (String) transactionData.get("commoditySpecification");
        if (commoditySpec == null || commoditySpec.trim().isEmpty()) {
            throw new FAPISecurityException("Detailed commodity specification required for Salam transaction",
                    "salam_validation_error");
        }

        // Validate delivery date
        String deliveryDate = (String) transactionData.get("deliveryDate");
        if (deliveryDate == null || deliveryDate.trim().isEmpty()) {
            throw new FAPISecurityException("Future delivery date required for Salam transaction",
                    "salam_validation_error");
        }
    }

    /**
     * Validate Istisna transaction specific requirements
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if validation fails
     */
    private void validateIstisnaTransaction(Object transaction) {
        Map<String, Object> transactionData = parseTransactionData(convertToJson(transaction));

        // Validate manufacturing specifications
        String manufacturingSpec = (String) transactionData.get("manufacturingSpecification");
        if (manufacturingSpec == null || manufacturingSpec.trim().isEmpty()) {
            throw new FAPISecurityException("Manufacturing specification required for Istisna transaction",
                    "istisna_validation_error");
        }

        // Validate delivery schedule
        String deliverySchedule = (String) transactionData.get("deliverySchedule");
        if (deliverySchedule == null || deliverySchedule.trim().isEmpty()) {
            throw new FAPISecurityException("Delivery schedule required for Istisna transaction",
                    "istisna_validation_error");
        }

        // Validate progress milestones
        String progressMilestones = (String) transactionData.get("progressMilestones");
        if (progressMilestones == null || progressMilestones.trim().isEmpty()) {
            throw new FAPISecurityException("Progress milestones required for Istisna transaction",
                    "istisna_validation_error");
        }
    }

    /**
     * Validate Qard Hassan transaction specific requirements
     * 
     * @param transaction the transaction to validate
     * @throws FAPISecurityException if validation fails
     */
    private void validateQardHassanTransaction(Object transaction) {
        Map<String, Object> transactionData = parseTransactionData(convertToJson(transaction));

        // Validate interest-free nature
        Double interestRate = (Double) transactionData.get("interestRate");
        if (interestRate != null && interestRate > 0) {
            throw new FAPISecurityException("Qard Hassan must be interest-free",
                    "qard_hassan_validation_error");
        }

        // Validate benevolent purpose
        String purpose = (String) transactionData.get("purpose");
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new FAPISecurityException("Purpose required for Qard Hassan transaction",
                    "qard_hassan_validation_error");
        }

        // Validate no additional charges
        Double additionalCharges = (Double) transactionData.get("additionalCharges");
        if (additionalCharges != null && additionalCharges > 0) {
            throw new FAPISecurityException("Additional charges not allowed in Qard Hassan",
                    "qard_hassan_validation_error");
        }
    }

    /**
     * Helper method to convert transaction to JSON
     * 
     * @param transaction the transaction object
     * @return JSON representation
     */
    private String convertToJson(Object transaction) {
        try {
            return transaction.toString(); // Simplified for demonstration
        } catch (Exception e) {
            throw new FAPISecurityException("Failed to convert transaction to JSON",
                    "json_conversion_error", e);
        }
    }

    /**
     * Helper method to parse transaction data
     * 
     * @param transactionJson the transaction JSON
     * @return parsed transaction data
     */
    private Map<String, Object> parseTransactionData(String transactionJson) {
        // Simplified parsing for demonstration
        return new HashMap<>();
    }

    /**
     * Create Islamic finance-specific FAPI error response
     * 
     * @param errorCode        the error code
     * @param errorDescription the error description
     * @param interactionId    the FAPI interaction ID
     * @return Islamic finance FAPI-compliant error response
     */
    public Map<String, Object> createIslamicFAPIErrorResponse(String errorCode, String errorDescription,
            String interactionId) {
        Map<String, Object> errorResponse = createFAPIErrorResponse(errorCode, errorDescription, interactionId);

        // Add Islamic finance context
        errorResponse.put("islamic_finance_framework", "MasruFi");
        errorResponse.put("sharia_compliance_required", true);
        errorResponse.put("compliance_authority", UAE_ISLAMIC_AUTHORITY);

        return errorResponse;
    }
}