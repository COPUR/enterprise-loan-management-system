package com.bank.loanmanagement.domain.model.berlingroup;

import com.bank.loanmanagement.domain.model.bian.BIANServiceDomain;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Berlin Group PSD2 Compliant Account Data Structure
 * 
 * Implements:
 * - Berlin Group NextGenPSD2 XS2A Framework
 * - PSD2 Account Information Service (AIS) requirements
 * - BIAN Service Domain alignment
 * - ISO 20022 message standards
 * - FAPI security requirements
 */
@Data
@Builder
@Jacksonized
public class BerlinGroupAccount implements BIANServiceDomain {
    
    /**
     * Unique identifier of the account assigned by the ASPSP (Account Servicing Payment Service Provider)
     * Format: Max35Text according to ISO 20022
     */
    @JsonProperty("resourceId")
    private String resourceId;
    
    /**
     * International Bank Account Number (IBAN) according to ISO 13616
     */
    @JsonProperty("iban")
    private String iban;
    
    /**
     * Basic Bank Account Number (BBAN) for national account identification
     */
    @JsonProperty("bban")
    private String bban;
    
    /**
     * Primary Account Number (PAN) for card accounts
     */
    @JsonProperty("pan")
    private String pan;
    
    /**
     * Masked Primary Account Number for display purposes
     */
    @JsonProperty("maskedPan")
    private String maskedPan;
    
    /**
     * Mobile Subscriber Integrated Services Digital Network Number (MSISDN)
     */
    @JsonProperty("msisdn")
    private String msisdn;
    
    /**
     * Account currency according to ISO 4217
     */
    @JsonProperty("currency")
    private String currency;
    
    /**
     * Human readable name of the account
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * Specification of the account type
     * Values: Cacc (Current), Svgs (Savings), Tran (TransactingAccount), etc.
     */
    @JsonProperty("cashAccountType")
    private String cashAccountType;
    
    /**
     * Account status according to Berlin Group specification
     */
    @JsonProperty("status")
    private AccountStatus status;
    
    /**
     * Bank Identifier Code (BIC) according to ISO 9362
     */
    @JsonProperty("bic")
    private String bic;
    
    /**
     * Name of the account holder
     */
    @JsonProperty("ownerName")
    private String ownerName;
    
    /**
     * Address of the account holder
     */
    @JsonProperty("ownerAddress")
    private BerlinGroupAddress ownerAddress;
    
    /**
     * Account details structure
     */
    @JsonProperty("details")
    private String details;
    
    /**
     * Available balance information
     */
    @JsonProperty("balances")
    private List<BerlinGroupBalance> balances;
    
    /**
     * Additional account information
     */
    @JsonProperty("_links")
    private Map<String, BerlinGroupLink> links;
    
    /**
     * BIAN Service Domain Information
     */
    @JsonProperty("bianServiceDomain")
    private BIANCurrentAccountDomain bianServiceDomain;
    
    /**
     * Account Status Enumeration according to Berlin Group
     */
    public enum AccountStatus {
        @JsonProperty("enabled")
        ENABLED,
        
        @JsonProperty("deleted")
        DELETED,
        
        @JsonProperty("blocked")
        BLOCKED
    }
    
    /**
     * Berlin Group compliant balance structure
     */
    @Data
    @Builder
    @Jacksonized
    public static class BerlinGroupBalance {
        
        /**
         * Balance type according to Berlin Group specification
         */
        @JsonProperty("balanceType")
        private BalanceType balanceType;
        
        /**
         * Balance amount with currency
         */
        @JsonProperty("balanceAmount")
        private BerlinGroupAmount balanceAmount;
        
        /**
         * Reference date of the balance
         */
        @JsonProperty("referenceDate")
        private LocalDate referenceDate;
        
        /**
         * Credit limit if applicable
         */
        @JsonProperty("creditLimitIncluded")
        private Boolean creditLimitIncluded;
        
        /**
         * Last action date on the account
         */
        @JsonProperty("lastActionDateTime")
        private String lastActionDateTime;
        
        public enum BalanceType {
            @JsonProperty("closingBooked")
            CLOSING_BOOKED,
            
            @JsonProperty("expected")
            EXPECTED,
            
            @JsonProperty("authorised")
            AUTHORISED,
            
            @JsonProperty("openingBooked")
            OPENING_BOOKED,
            
            @JsonProperty("interimAvailable")
            INTERIM_AVAILABLE,
            
            @JsonProperty("interimBooked")
            INTERIM_BOOKED,
            
            @JsonProperty("forwardAvailable")
            FORWARD_AVAILABLE,
            
            @JsonProperty("nonInvoiced")
            NON_INVOICED
        }
    }
    
    /**
     * Berlin Group compliant amount structure
     */
    @Data
    @Builder
    @Jacksonized
    public static class BerlinGroupAmount {
        
        /**
         * Currency code according to ISO 4217
         */
        @JsonProperty("currency")
        private String currency;
        
        /**
         * Amount value as string to preserve precision
         */
        @JsonProperty("amount")
        private String amount;
        
        /**
         * Convert to BigDecimal for calculations
         */
        public BigDecimal getAmountAsBigDecimal() {
            return new BigDecimal(amount);
        }
    }
    
    /**
     * Berlin Group compliant address structure
     */
    @Data
    @Builder
    @Jacksonized
    public static class BerlinGroupAddress {
        
        @JsonProperty("streetName")
        private String streetName;
        
        @JsonProperty("buildingNumber")
        private String buildingNumber;
        
        @JsonProperty("townName")
        private String townName;
        
        @JsonProperty("postCode")
        private String postCode;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("addressLine")
        private List<String> addressLine;
    }
    
    /**
     * Berlin Group compliant link structure for HATEOAS
     */
    @Data
    @Builder
    @Jacksonized
    public static class BerlinGroupLink {
        
        @JsonProperty("href")
        private String href;
        
        @JsonProperty("rel")
        private String rel;
        
        @JsonProperty("type")
        private String type;
    }
    
    /**
     * BIAN Current Account Service Domain Integration
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANCurrentAccountDomain {
        
        /**
         * BIAN Service Domain Reference
         */
        @JsonProperty("serviceDomainReference")
        private String serviceDomainReference;
        
        /**
         * Current Account Facility Reference
         */
        @JsonProperty("currentAccountFacilityReference")
        private String currentAccountFacilityReference;
        
        /**
         * Account configuration including limits and features
         */
        @JsonProperty("accountConfiguration")
        private BIANAccountConfiguration accountConfiguration;
        
        /**
         * Position keeping for real-time balance management
         */
        @JsonProperty("positionKeeping")
        private BIANPositionKeeping positionKeeping;
        
        /**
         * Interest calculation parameters
         */
        @JsonProperty("interestCalculation")
        private BIANInterestCalculation interestCalculation;
        
        /**
         * Service fees configuration
         */
        @JsonProperty("serviceFees")
        private BIANServiceFees serviceFees;
    }
    
    /**
     * BIAN Account Configuration
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANAccountConfiguration {
        
        @JsonProperty("accountType")
        private String accountType;
        
        @JsonProperty("accountPurpose")
        private String accountPurpose;
        
        @JsonProperty("taxReference")
        private String taxReference;
        
        @JsonProperty("entitlementOptionDefinition")
        private String entitlementOptionDefinition;
        
        @JsonProperty("restrictionOptionDefinition")
        private String restrictionOptionDefinition;
        
        @JsonProperty("associations")
        private List<String> associations;
        
        @JsonProperty("linkType")
        private String linkType;
        
        @JsonProperty("accountDetails")
        private String accountDetails;
    }
    
    /**
     * BIAN Position Keeping for real-time balance management
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANPositionKeeping {
        
        @JsonProperty("positionLimitType")
        private String positionLimitType;
        
        @JsonProperty("positionLimitSettings")
        private BigDecimal positionLimitSettings;
        
        @JsonProperty("positionLimitValue")
        private BigDecimal positionLimitValue;
        
        @JsonProperty("amountBlock")
        private BerlinGroupAmount amountBlock;
        
        @JsonProperty("amountBlockType")
        private String amountBlockType;
        
        @JsonProperty("priority")
        private String priority;
        
        @JsonProperty("dateType")
        private String dateType;
        
        @JsonProperty("amount")
        private BerlinGroupAmount amount;
        
        @JsonProperty("amountType")
        private String amountType;
    }
    
    /**
     * BIAN Interest Calculation
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANInterestCalculation {
        
        @JsonProperty("interestRateType")
        private String interestRateType;
        
        @JsonProperty("interestRate")
        private BigDecimal interestRate;
        
        @JsonProperty("accrualAmount")
        private BerlinGroupAmount accrualAmount;
        
        @JsonProperty("accrualDate")
        private LocalDate accrualDate;
        
        @JsonProperty("accrualType")
        private String accrualType;
        
        @JsonProperty("processingOptionDefinition")
        private String processingOptionDefinition;
    }
    
    /**
     * BIAN Service Fees
     */
    @Data
    @Builder
    @Jacksonized
    public static class BIANServiceFees {
        
        @JsonProperty("feeConfigurationProfile")
        private String feeConfigurationProfile;
        
        @JsonProperty("feeDefinition")
        private String feeDefinition;
        
        @JsonProperty("feeApplicationRecord")
        private String feeApplicationRecord;
        
        @JsonProperty("feeTransaction")
        private String feeTransaction;
        
        @JsonProperty("transactionDescription")
        private String transactionDescription;
        
        @JsonProperty("transactionFeeType")
        private String transactionFeeType;
        
        @JsonProperty("transactionFeeCharge")
        private BerlinGroupAmount transactionFeeCharge;
    }
    
    @Override
    public String getServiceDomainReference() {
        return bianServiceDomain != null ? bianServiceDomain.getServiceDomainReference() : null;
    }
    
    @Override
    public String getBIANFunctionalPattern() {
        return "Fulfill"; // Current Account follows the Fulfill pattern
    }
    
    @Override
    public String getBIANBusinessArea() {
        return "Customer Products & Services";
    }
    
    /**
     * Validate Berlin Group PSD2 compliance
     */
    public boolean isPSD2Compliant() {
        return resourceId != null && 
               (iban != null || bban != null || pan != null) &&
               currency != null &&
               status != null &&
               balances != null && !balances.isEmpty();
    }
    
    /**
     * Get the primary account identifier following Berlin Group priority
     */
    public String getPrimaryAccountIdentifier() {
        if (iban != null) return iban;
        if (bban != null) return bban;
        if (pan != null) return pan;
        if (msisdn != null) return msisdn;
        return resourceId;
    }
    
    /**
     * Get available balance according to Berlin Group specification
     */
    public BerlinGroupAmount getAvailableBalance() {
        return balances.stream()
            .filter(balance -> balance.getBalanceType() == BerlinGroupBalance.BalanceType.INTERIM_AVAILABLE)
            .findFirst()
            .map(BerlinGroupBalance::getBalanceAmount)
            .orElse(null);
    }
    
    /**
     * Get booked balance according to Berlin Group specification
     */
    public BerlinGroupAmount getBookedBalance() {
        return balances.stream()
            .filter(balance -> balance.getBalanceType() == BerlinGroupBalance.BalanceType.CLOSING_BOOKED)
            .findFirst()
            .map(BerlinGroupBalance::getBalanceAmount)
            .orElse(null);
    }
}