package com.bank.loanmanagement.domain.shared;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;

/**
 * Berlin Group (NextGenPSD2) compliant data types
 * Following PSD2 API standards for payment services
 * Ensures FAPI compliance and regulatory adherence
 */
public final class BerlinGroupTypes {

    /**
     * Amount as defined by Berlin Group specification
     * ISO 20022 compliant monetary amount
     */
    @Value
    public static class Amount {
        String currency;  // ISO 4217 currency code
        String amount;    // Decimal representation as string

        public static Amount of(BigDecimal value, Currency currency) {
            return new Amount(currency.getCurrencyCode(), value.toPlainString());
        }

        public static Amount of(String amount, String currency) {
            return new Amount(currency, amount);
        }

        public BigDecimal toBigDecimal() {
            return new BigDecimal(amount);
        }
    }

    /**
     * Account Reference following Berlin Group specification
     * Supports IBAN, BBAN, PAN, and proprietary account identifiers
     */
    @Value
    public static class AccountReference {
        String iban;           // ISO 13616 IBAN
        String bban;           // Basic Bank Account Number
        String pan;            // Primary Account Number
        String maskedPan;      // Masked Primary Account Number
        String msisdn;         // Mobile phone number for mobile payments
        String currency;       // ISO 4217 currency code
        String cashAccountType; // ExternalCashAccountType1Code

        public static AccountReference iban(String iban, String currency) {
            return new AccountReference(iban, null, null, null, null, currency, null);
        }

        public static AccountReference bban(String bban, String currency) {
            return new AccountReference(null, bban, null, null, null, currency, null);
        }
    }

    /**
     * Address following Berlin Group specification
     * ISO 20022 PostalAddress24 compliant
     */
    @Value
    public static class Address {
        String streetName;
        String buildingNumber;
        String townName;
        String postCode;
        String country;         // ISO 3166 ALPHA2 country code

        public static Address of(String streetName, String buildingNumber, 
                               String townName, String postCode, String country) {
            return new Address(streetName, buildingNumber, townName, postCode, country);
        }
    }

    /**
     * Creditor/Debtor information following Berlin Group
     * ISO 20022 PartyIdentification135 compliant
     */
    @Value
    public static class PartyIdentification {
        String name;           // Max 70 characters
        Address postalAddress;
        String organisationId; // Organisation identifier
        String privateId;      // Private person identifier

        public static PartyIdentification person(String name, Address address, String privateId) {
            return new PartyIdentification(name, address, null, privateId);
        }

        public static PartyIdentification organisation(String name, Address address, String organisationId) {
            return new PartyIdentification(name, address, organisationId, null);
        }
    }

    /**
     * Transaction Status following Berlin Group
     * ISO 20022 TransactionStatus4Code compliant
     */
    public enum TransactionStatus {
        ACCP, // AcceptedCustomerProfile
        ACSC, // AcceptedSettlementCompleted
        ACSP, // AcceptedSettlementInProcess
        ACTC, // AcceptedTechnicalValidation
        ACWC, // AcceptedWithChange
        ACWP, // AcceptedWithoutPosting
        RCVD, // Received
        PDNG, // Pending
        RJCT, // Rejected
        CANC, // Cancelled
        ACFC, // AcceptedFundsChecked
        PATC, // PartiallyAcceptedTechnicalCorrect
        PART  // PartiallyAccepted
    }

    /**
     * Authentication Object following Berlin Group SCA
     * Strong Customer Authentication compliant
     */
    @Value
    public static class AuthenticationObject {
        String authenticationType;  // SMS_OTP, CHIP_OTP, PHOTO_OTP, PUSH_OTP
        String authenticationVersion;
        String authenticationMethodId;
        String name;
        String explanation;

        public static AuthenticationObject smsOtp(String methodId, String explanation) {
            return new AuthenticationObject("SMS_OTP", "1.0", methodId, "SMS OTP", explanation);
        }

        public static AuthenticationObject pushOtp(String methodId, String explanation) {
            return new AuthenticationObject("PUSH_OTP", "1.0", methodId, "Push OTP", explanation);
        }
    }

    /**
     * SCA Status following Berlin Group
     * Strong Customer Authentication status
     */
    public enum ScaStatus {
        RECEIVED,           // SCA method selection
        PSUIDENTIFIED,      // PSU identified
        PSUAUTHENTICATED,   // PSU authenticated
        SCAMETHODSELECTED,  // SCA method selected
        STARTED,            // SCA started
        FINALISED,          // SCA finalised
        FAILED,             // SCA failed
        EXEMPTED            // SCA exempted
    }

    /**
     * Frequency Code following Berlin Group
     * ISO 20022 Frequency36Choice compliant
     */
    public enum FrequencyCode {
        DAILY,    // Daily
        WEEKLY,   // Weekly
        FORTNIGHTLY, // Every two weeks
        MONTHLY,  // Monthly
        QUARTERLY, // Quarterly
        SEMIANNUAL, // Semi-annual
        ANNUAL    // Annual
    }

    /**
     * Day of Execution following Berlin Group
     * Used for standing orders and periodic payments
     */
    @Value
    public static class DayOfExecution {
        String dayOfExecution; // 01-31 for monthly, 1-7 for weekly

        public static DayOfExecution monthly(int day) {
            if (day < 1 || day > 31) {
                throw new IllegalArgumentException("Day must be between 1 and 31");
            }
            return new DayOfExecution(String.format("%02d", day));
        }

        public static DayOfExecution weekly(int dayOfWeek) {
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw new IllegalArgumentException("Day of week must be between 1 and 7");
            }
            return new DayOfExecution(String.valueOf(dayOfWeek));
        }
    }

    /**
     * Remittance Information following Berlin Group
     * ISO 20022 RemittanceInformation16 compliant
     */
    @Value
    public static class RemittanceInformation {
        String unstructured;    // Max 140 characters
        String reference;       // Structured reference
        String referenceType;   // Reference type
        String referenceIssuer; // Reference issuer

        public static RemittanceInformation unstructured(String text) {
            if (text != null && text.length() > 140) {
                throw new IllegalArgumentException("Unstructured remittance info cannot exceed 140 characters");
            }
            return new RemittanceInformation(text, null, null, null);
        }

        public static RemittanceInformation structured(String reference, String type, String issuer) {
            return new RemittanceInformation(null, reference, type, issuer);
        }
    }

    /**
     * Purpose Code following Berlin Group
     * ISO 20022 ExternalPurpose1Code compliant
     */
    public enum PurposeCode {
        CBFF, // Capital Building Deposit
        CDCD, // Credit Card Payment
        CHAR, // Charity Payment
        COMC, // Commercial Payment
        CPKC, // Car Parking Fee
        DIVI, // Dividend
        GOVI, // Government Insurance
        GSTP, // Goods and Service Tax Payment
        INST, // Installment
        INTC, // Intra Company Payment
        LIMA, // Liquidity Management
        OTHR, // Other
        RLTI, // Real Time
        SALA, // Salary Payment
        SECU, // Securities
        SSBE, // Social Security Benefit
        SUPP, // Supplier Payment
        TAXS, // Tax Payment
        TRAD, // Trade Services
        TREA, // Treasury Payment
        VATX, // Value Added Tax Payment
        WHLD  // Withholding
    }

    /**
     * Links following Berlin Group HATEOAS
     * Self-referential links for API navigation
     */
    @Value
    public static class Links {
        String self;
        String status;
        String scaRedirect;
        String scaOAuth;
        String confirmation;
        String startAuthorisation;
        String startAuthorisationWithPsuIdentification;
        String startAuthorisationWithPsuAuthentication;
        String startAuthorisationWithEncryptedPsuAuthentication;
        String startAuthorisationWithAuthenticationMethodSelection;
        String selectAuthenticationMethod;
        String authoriseTransaction;

        public static LinksBuilder builder() {
            return new LinksBuilder();
        }

        public static class LinksBuilder {
            private String self;
            private String status;
            private String scaRedirect;
            private String scaOAuth;
            private String confirmation;
            private String startAuthorisation;
            private String startAuthorisationWithPsuIdentification;
            private String startAuthorisationWithPsuAuthentication;
            private String startAuthorisationWithEncryptedPsuAuthentication;
            private String startAuthorisationWithAuthenticationMethodSelection;
            private String selectAuthenticationMethod;
            private String authoriseTransaction;

            public LinksBuilder self(String self) { this.self = self; return this; }
            public LinksBuilder status(String status) { this.status = status; return this; }
            public LinksBuilder scaRedirect(String scaRedirect) { this.scaRedirect = scaRedirect; return this; }
            public LinksBuilder scaOAuth(String scaOAuth) { this.scaOAuth = scaOAuth; return this; }
            public LinksBuilder confirmation(String confirmation) { this.confirmation = confirmation; return this; }
            public LinksBuilder startAuthorisation(String startAuthorisation) { this.startAuthorisation = startAuthorisation; return this; }
            public LinksBuilder startAuthorisationWithPsuIdentification(String startAuthorisationWithPsuIdentification) { this.startAuthorisationWithPsuIdentification = startAuthorisationWithPsuIdentification; return this; }
            public LinksBuilder startAuthorisationWithPsuAuthentication(String startAuthorisationWithPsuAuthentication) { this.startAuthorisationWithPsuAuthentication = startAuthorisationWithPsuAuthentication; return this; }
            public LinksBuilder startAuthorisationWithEncryptedPsuAuthentication(String startAuthorisationWithEncryptedPsuAuthentication) { this.startAuthorisationWithEncryptedPsuAuthentication = startAuthorisationWithEncryptedPsuAuthentication; return this; }
            public LinksBuilder startAuthorisationWithAuthenticationMethodSelection(String startAuthorisationWithAuthenticationMethodSelection) { this.startAuthorisationWithAuthenticationMethodSelection = startAuthorisationWithAuthenticationMethodSelection; return this; }
            public LinksBuilder selectAuthenticationMethod(String selectAuthenticationMethod) { this.selectAuthenticationMethod = selectAuthenticationMethod; return this; }
            public LinksBuilder authoriseTransaction(String authoriseTransaction) { this.authoriseTransaction = authoriseTransaction; return this; }

            public Links build() {
                return new Links(self, status, scaRedirect, scaOAuth, confirmation, startAuthorisation,
                        startAuthorisationWithPsuIdentification, startAuthorisationWithPsuAuthentication,
                        startAuthorisationWithEncryptedPsuAuthentication, startAuthorisationWithAuthenticationMethodSelection,
                        selectAuthenticationMethod, authoriseTransaction);
            }
        }
    }

    /**
     * PSU Data following Berlin Group
     * Payment Service User identification
     */
    @Value
    public static class PsuData {
        String psuId;                    // PSU identifier
        String psuIdType;                // Type of PSU identifier
        String psuCorporateId;           // Corporate PSU identifier
        String psuCorporateIdType;       // Type of corporate PSU identifier
        String psuIpAddress;             // PSU IP address (mandatory)
        String psuIpPort;                // PSU IP port
        String psuUserAgent;             // PSU user agent
        String psuGeoLocation;           // PSU geographic location
        OffsetDateTime psuHttpMethod;    // PSU HTTP method timestamp
        String psuDeviceId;              // PSU device identifier
        OffsetDateTime psuAccept;        // PSU accept timestamp
        String psuAcceptCharset;         // PSU accept charset
        String psuAcceptEncoding;        // PSU accept encoding
        String psuAcceptLanguage;        // PSU accept language

        public static PsuDataBuilder builder() {
            return new PsuDataBuilder();
        }

        public static class PsuDataBuilder {
            private String psuId;
            private String psuIdType;
            private String psuCorporateId;
            private String psuCorporateIdType;
            private String psuIpAddress;
            private String psuIpPort;
            private String psuUserAgent;
            private String psuGeoLocation;
            private OffsetDateTime psuHttpMethod;
            private String psuDeviceId;
            private OffsetDateTime psuAccept;
            private String psuAcceptCharset;
            private String psuAcceptEncoding;
            private String psuAcceptLanguage;

            public PsuDataBuilder psuId(String psuId) { this.psuId = psuId; return this; }
            public PsuDataBuilder psuIdType(String psuIdType) { this.psuIdType = psuIdType; return this; }
            public PsuDataBuilder psuCorporateId(String psuCorporateId) { this.psuCorporateId = psuCorporateId; return this; }
            public PsuDataBuilder psuCorporateIdType(String psuCorporateIdType) { this.psuCorporateIdType = psuCorporateIdType; return this; }
            public PsuDataBuilder psuIpAddress(String psuIpAddress) { this.psuIpAddress = psuIpAddress; return this; }
            public PsuDataBuilder psuIpPort(String psuIpPort) { this.psuIpPort = psuIpPort; return this; }
            public PsuDataBuilder psuUserAgent(String psuUserAgent) { this.psuUserAgent = psuUserAgent; return this; }
            public PsuDataBuilder psuGeoLocation(String psuGeoLocation) { this.psuGeoLocation = psuGeoLocation; return this; }
            public PsuDataBuilder psuHttpMethod(OffsetDateTime psuHttpMethod) { this.psuHttpMethod = psuHttpMethod; return this; }
            public PsuDataBuilder psuDeviceId(String psuDeviceId) { this.psuDeviceId = psuDeviceId; return this; }
            public PsuDataBuilder psuAccept(OffsetDateTime psuAccept) { this.psuAccept = psuAccept; return this; }
            public PsuDataBuilder psuAcceptCharset(String psuAcceptCharset) { this.psuAcceptCharset = psuAcceptCharset; return this; }
            public PsuDataBuilder psuAcceptEncoding(String psuAcceptEncoding) { this.psuAcceptEncoding = psuAcceptEncoding; return this; }
            public PsuDataBuilder psuAcceptLanguage(String psuAcceptLanguage) { this.psuAcceptLanguage = psuAcceptLanguage; return this; }

            public PsuData build() {
                return new PsuData(psuId, psuIdType, psuCorporateId, psuCorporateIdType, psuIpAddress, psuIpPort,
                        psuUserAgent, psuGeoLocation, psuHttpMethod, psuDeviceId, psuAccept, psuAcceptCharset,
                        psuAcceptEncoding, psuAcceptLanguage);
            }
        }
    }

    /**
     * Tpp Info following Berlin Group
     * Third Party Provider information
     */
    @Value
    public static class TppInfo {
        String tppId;                    // TPP identifier
        String tppName;                  // TPP name
        String tppRole;                  // TPP role (AISP, PISP, CBPII)
        String tppNationalCompetentAuthority; // National Competent Authority
        String tppRedirectUri;           // TPP redirect URI
        String tppNokRedirectUri;        // TPP Nok redirect URI
        boolean tppExplicitAuthorisationPreferred; // Explicit authorisation preferred

        public static TppInfoBuilder builder() {
            return new TppInfoBuilder();
        }

        public static class TppInfoBuilder {
            private String tppId;
            private String tppName;
            private String tppRole;
            private String tppNationalCompetentAuthority;
            private String tppRedirectUri;
            private String tppNokRedirectUri;
            private boolean tppExplicitAuthorisationPreferred;

            public TppInfoBuilder tppId(String tppId) { this.tppId = tppId; return this; }
            public TppInfoBuilder tppName(String tppName) { this.tppName = tppName; return this; }
            public TppInfoBuilder tppRole(String tppRole) { this.tppRole = tppRole; return this; }
            public TppInfoBuilder tppNationalCompetentAuthority(String tppNationalCompetentAuthority) { this.tppNationalCompetentAuthority = tppNationalCompetentAuthority; return this; }
            public TppInfoBuilder tppRedirectUri(String tppRedirectUri) { this.tppRedirectUri = tppRedirectUri; return this; }
            public TppInfoBuilder tppNokRedirectUri(String tppNokRedirectUri) { this.tppNokRedirectUri = tppNokRedirectUri; return this; }
            public TppInfoBuilder tppExplicitAuthorisationPreferred(boolean tppExplicitAuthorisationPreferred) { this.tppExplicitAuthorisationPreferred = tppExplicitAuthorisationPreferred; return this; }

            public TppInfo build() {
                return new TppInfo(tppId, tppName, tppRole, tppNationalCompetentAuthority, tppRedirectUri,
                        tppNokRedirectUri, tppExplicitAuthorisationPreferred);
            }
        }
    }
}