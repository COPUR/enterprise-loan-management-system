package com.bank.loanmanagement.loan.domain.shared;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * BIAN (Banking Industry Architecture Network) compliant data types
 * Following BIAN Service Domain architecture patterns
 * Ensures standardized banking business capabilities
 */
public final class BianTypes {

    /**
     * BIAN Service Domain Instance Reference
     * Unique identifier for service domain instances
     */
    public record ServiceDomainInstanceReference(
        String serviceDomainName,
        String instanceReference,
        String version
    ) {
        public static ServiceDomainInstanceReference of(String serviceDomain, String instance, String version) {
            return new ServiceDomainInstanceReference(serviceDomain, instance, version);
        }
    }

    /**
     * BIAN Control Record
     * Standard control information for all BIAN operations
     */
    public record ControlRecord(
        String controlRecordType,
        String controlRecordInstanceReference,
        String controlRecordDateTime,
        String controlRecordLocation
    ) {
        public static ControlRecord of(String type, String reference, String dateTime, String location) {
            return new ControlRecord(type, reference, dateTime, location);
        }
    }

    /**
     * BIAN General Business Unit Reference
     * Reference to business units within the bank
     */
    public record GeneralBusinessUnitReference(
        String businessUnitType,
        String businessUnitReference,
        String businessUnitName
    ) {
        public static GeneralBusinessUnitReference of(String type, String reference, String name) {
            return new GeneralBusinessUnitReference(type, reference, name);
        }
    }

    /**
     * BIAN Employee/Business Unit Reference
     * Reference to employees or business units
     */
    public record EmployeeBusinessUnitReference(
        String employeeBusinessUnitReference,
        String employeeBusinessUnitType
    ) {
        public static EmployeeBusinessUnitReference of(String reference, String type) {
            return new EmployeeBusinessUnitReference(reference, type);
        }
    }

    /**
     * BIAN Customer Reference
     * Standardized customer identification
     */
    public record CustomerReference(
        String customerReference,
        String customerType,
        String customerSegment
    ) {
        public static CustomerReference of(String reference, String type, String segment) {
            return new CustomerReference(reference, type, segment);
        }
    }

    /**
     * BIAN Product/Service Type
     * Standardized product and service classification
     */
    public record ProductServiceType(
        String productServiceType,
        String productServiceTypeDefinition,
        String productServiceFeature
    ) {
        public static ProductServiceType of(String type, String definition, String feature) {
            return new ProductServiceType(type, definition, feature);
        }
    }

    /**
     * BIAN Date Type
     * Standardized date handling across BIAN domains
     */
    public record DateType(
        String dateType,
        LocalDate date
    ) {
        public static DateType of(String type, LocalDate date) {
            return new DateType(type, date);
        }

        public static DateType today(String type) {
            return new DateType(type, LocalDate.now());
        }
    }

    /**
     * BIAN Currency And Amount
     * Monetary amount with currency specification
     */
    public record CurrencyAndAmount(
        String currencyCode,    // ISO 4217
        BigDecimal amount
    ) {
        public static CurrencyAndAmount of(String currency, BigDecimal amount) {
            return new CurrencyAndAmount(currency, amount);
        }

        public static CurrencyAndAmount of(String currency, String amount) {
            return new CurrencyAndAmount(currency, new BigDecimal(amount));
        }
    }

    /**
     * BIAN Rate
     * Interest rates, fees, and other percentage values
     */
    public record Rate(
        String rateType,
        BigDecimal rateValue,
        String rateUnit,        // Percentage, Basis Points, etc.
        String ratePeriod      // Annual, Monthly, Daily, etc.
    ) {
        public static Rate percentage(String type, BigDecimal value, String period) {
            return new Rate(type, value, "Percentage", period);
        }

        public static Rate basisPoints(String type, BigDecimal value, String period) {
            return new Rate(type, value, "BasisPoints", period);
        }
    }

    /**
     * BIAN Text
     * Standardized text handling with type classification
     */
    public record Text(
        String textType,
        String textString,
        String textLanguage    // ISO 639-1
    ) {
        public static Text of(String type, String text, String language) {
            return new Text(type, text, language);
        }

        public static Text english(String type, String text) {
            return new Text(type, text, "en");
        }
    }

    /**
     * BIAN Instruction
     * Standardized instruction format
     */
    public record Instruction(
        String instructionType,
        String instructionDescription,
        String instructionFormat,
        OffsetDateTime instructionDateTime
    ) {
        public static Instruction of(String type, String description, String format, OffsetDateTime dateTime) {
            return new Instruction(type, description, format, dateTime);
        }

        public static Instruction now(String type, String description, String format) {
            return new Instruction(type, description, format, OffsetDateTime.now());
        }
    }

    /**
     * BIAN Document Reference
     * Reference to documents and their classification
     */
    public record DocumentReference(
        String documentType,
        String documentReference,
        String documentStatus,
        String documentLocation
    ) {
        public static DocumentReference of(String type, String reference, String status, String location) {
            return new DocumentReference(type, reference, status, location);
        }
    }

    /**
     * BIAN Assessment
     * Standardized assessment results
     */
    public record Assessment(
        String assessmentType,
        String assessmentDescription,
        String assessmentResult,
        OffsetDateTime assessmentDateTime,
        EmployeeBusinessUnitReference assessedBy
    ) {
        public static Assessment of(String type, String description, String result, 
                                  OffsetDateTime dateTime, EmployeeBusinessUnitReference assessedBy) {
            return new Assessment(type, description, result, dateTime, assessedBy);
        }
    }

    /**
     * BIAN Action
     * Standardized action specification
     */
    public record Action(
        String actionType,
        String actionDescription,
        String actionStatus,
        OffsetDateTime actionDateTime,
        EmployeeBusinessUnitReference actionBy
    ) {
        public static Action of(String type, String description, String status, 
                              OffsetDateTime dateTime, EmployeeBusinessUnitReference actionBy) {
            return new Action(type, description, status, dateTime, actionBy);
        }
    }

    /**
     * BIAN Feature
     * Product or service features
     */
    public record Feature(
        String featureType,
        String featureDefinition,
        String featureConfiguration,
        String featureStatus
    ) {
        public static Feature of(String type, String definition, String configuration, String status) {
            return new Feature(type, definition, configuration, status);
        }
    }

    /**
     * BIAN Limit
     * Various types of limits (credit, transaction, etc.)
     */
    public record Limit(
        String limitType,
        CurrencyAndAmount limitAmount,
        String limitPeriod,
        LocalDate limitEffectiveDate,
        LocalDate limitExpiryDate
    ) {
        public static Limit of(String type, CurrencyAndAmount amount, String period, 
                             LocalDate effectiveDate, LocalDate expiryDate) {
            return new Limit(type, amount, period, effectiveDate, expiryDate);
        }
    }

    /**
     * BIAN Schedule
     * Standardized scheduling information
     */
    public record Schedule(
        String scheduleType,
        String scheduleDefinition,
        LocalDate scheduleStartDate,
        LocalDate scheduleEndDate,
        String scheduleFrequency
    ) {
        public static Schedule of(String type, String definition, LocalDate startDate, 
                                LocalDate endDate, String frequency) {
            return new Schedule(type, definition, startDate, endDate, frequency);
        }
    }

    /**
     * BIAN Location Reference
     * Geographic location reference
     */
    public record LocationReference(
        String locationType,
        String locationReference,
        String locationName,
        String locationAddress
    ) {
        public static LocationReference of(String type, String reference, String name, String address) {
            return new LocationReference(type, reference, name, address);
        }
    }

    /**
     * BIAN Transaction
     * Standardized transaction structure
     */
    public record Transaction(
        String transactionType,
        String transactionReference,
        CurrencyAndAmount transactionAmount,
        OffsetDateTime transactionDateTime,
        String transactionStatus,
        String transactionDescription
    ) {
        public static Transaction of(String type, String reference, CurrencyAndAmount amount, 
                                   OffsetDateTime dateTime, String status, String description) {
            return new Transaction(type, reference, amount, dateTime, status, description);
        }
    }

    /**
     * BIAN Agreement
     * Standardized agreement/contract structure
     */
    public record Agreement(
        String agreementType,
        String agreementReference,
        String agreementDescription,
        LocalDate agreementEffectiveDate,
        LocalDate agreementExpiryDate,
        String agreementStatus,
        Text agreementTermsAndConditions
    ) {
        public static Agreement of(String type, String reference, String description, 
                                 LocalDate effectiveDate, LocalDate expiryDate, 
                                 String status, Text termsAndConditions) {
            return new Agreement(type, reference, description, effectiveDate, 
                               expiryDate, status, termsAndConditions);
        }
    }

    /**
     * BIAN Service Domain Activity Log
     * Audit trail for service domain activities
     */
    public record ServiceDomainActivityLog(
        String activityLogType,
        String activityLogReference,
        OffsetDateTime activityLogDateTime,
        EmployeeBusinessUnitReference activityLogBy,
        String activityLogDescription,
        String activityLogStatus
    ) {
        public static ServiceDomainActivityLog of(String type, String reference, OffsetDateTime dateTime,
                                                EmployeeBusinessUnitReference logBy, String description, String status) {
            return new ServiceDomainActivityLog(type, reference, dateTime, logBy, description, status);
        }
    }

    /**
     * BIAN Behavior Qualifier
     * Behavioral aspects of business processes
     */
    public enum BehaviorQualifier {
        INITIATE,    // Start a new instance
        UPDATE,      // Update an existing instance
        EXCHANGE,    // Exchange information
        EXECUTE,     // Execute a process
        REQUEST,     // Request information
        RETRIEVE,    // Retrieve information
        NOTIFY,      // Send notification
        CONTROL,     // Control process execution
        CAPTURE,     // Capture information
        GRANT,       // Grant permission/access
        PROVIDE,     // Provide service
        REGISTER,    // Register information
        CONFIGURE,   // Configure settings
        ACTIVATE,    // Activate functionality
        SUSPEND,     // Suspend operation
        TERMINATE    // Terminate process
    }

    /**
     * BIAN Function Type
     * Types of functions within service domains
     */
    public enum FunctionType {
        ADMINISTRATION,      // Administrative functions
        FULFILLMENT,        // Service fulfillment
        PRODUCTION,         // Production processes
        ANALYSIS,           // Analysis and reporting
        DESIGN,             // Design and configuration
        OVERSIGHT,          // Oversight and governance
        COMPLIANCE,         // Compliance monitoring
        MAINTENANCE,        // Maintenance operations
        DEVELOPMENT,        // Development activities
        OPERATIONS          // Operational functions
    }

    /**
     * BIAN Service Domain Configuration
     * Configuration parameters for service domains
     */
    public record ServiceDomainConfiguration(
        ServiceDomainInstanceReference serviceDomainInstanceReference,
        String configurationParameterType,
        String configurationParameterDescription,
        String configurationParameterSetting
    ) {
        public static ServiceDomainConfiguration of(ServiceDomainInstanceReference instanceRef,
                                                  String parameterType, String description, String setting) {
            return new ServiceDomainConfiguration(instanceRef, parameterType, description, setting);
        }
    }
}