package com.bank.loanmanagement.loan.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Comprehensive exception catalogue for banking operations with multi-language support
 * Implements error codes compliant with Berlin Group PSD2 and ISO 20022 standards
 */
@Component
@RequiredArgsConstructor
public class BankingExceptionCatalogue {

    private final MessageSource messageSource;

    @Getter
    @RequiredArgsConstructor
    public enum ErrorCode {
        // Authentication and Authorization Errors (1000-1999)
        AUTHENTICATION_FAILED("AUTH_001", "authentication.failed", 401),
        AUTHORIZATION_FAILED("AUTH_002", "authorization.failed", 403),
        TOKEN_EXPIRED("AUTH_003", "token.expired", 401),
        TOKEN_INVALID("AUTH_004", "token.invalid", 401),
        FAPI_COMPLIANCE_VIOLATION("AUTH_005", "fapi.compliance.violation", 400),
        DPOP_TOKEN_MISSING("AUTH_006", "dpop.token.missing", 400),
        PKCE_VALIDATION_FAILED("AUTH_007", "pkce.validation.failed", 400),
        INSUFFICIENT_PRIVILEGES("AUTH_008", "insufficient.privileges", 403),
        
        // Business Logic Errors (2000-2999)
        LOAN_NOT_FOUND("LOAN_001", "loan.not.found", 404),
        LOAN_ALREADY_EXISTS("LOAN_002", "loan.already.exists", 409),
        LOAN_AMOUNT_INVALID("LOAN_003", "loan.amount.invalid", 400),
        LOAN_INSTALLMENTS_INVALID("LOAN_004", "loan.installments.invalid", 400),
        LOAN_STATUS_INVALID("LOAN_005", "loan.status.invalid", 400),
        LOAN_APPROVAL_FAILED("LOAN_006", "loan.approval.failed", 422),
        LOAN_PAYMENT_FAILED("LOAN_007", "loan.payment.failed", 422),
        CUSTOMER_NOT_FOUND("CUST_001", "customer.not.found", 404),
        CUSTOMER_INACTIVE("CUST_002", "customer.inactive", 422),
        CUSTOMER_BLOCKED("CUST_003", "customer.blocked", 422),
        PAYMENT_FAILED("PAY_001", "payment.failed", 422),
        PAYMENT_AMOUNT_INVALID("PAY_002", "payment.amount.invalid", 400),
        PAYMENT_INSUFFICIENT_FUNDS("PAY_003", "payment.insufficient.funds", 422),
        
        // AI and Machine Learning Errors (3000-3999)
        AI_SERVICE_UNAVAILABLE("AI_001", "ai.service.unavailable", 503),
        AI_MODEL_ERROR("AI_002", "ai.model.error", 500),
        AI_FRAUD_DETECTION_FAILED("AI_003", "ai.fraud.detection.failed", 500),
        AI_RAG_QUERY_FAILED("AI_004", "ai.rag.query.failed", 500),
        AI_RECOMMENDATION_FAILED("AI_005", "ai.recommendation.failed", 500),
        VECTOR_DATABASE_ERROR("AI_006", "vector.database.error", 500),
        LLM_TIMEOUT("AI_007", "llm.timeout", 408),
        AI_COMPLIANCE_CHECK_FAILED("AI_008", "ai.compliance.check.failed", 500),
        
        // Data Validation Errors (4000-4999)
        VALIDATION_FAILED("VAL_001", "validation.failed", 400),
        BERLIN_GROUP_VALIDATION_FAILED("VAL_002", "berlin.group.validation.failed", 400),
        BIAN_VALIDATION_FAILED("VAL_003", "bian.validation.failed", 400),
        ISO20022_VALIDATION_FAILED("VAL_004", "iso20022.validation.failed", 400),
        REQUIRED_FIELD_MISSING("VAL_005", "required.field.missing", 400),
        FIELD_FORMAT_INVALID("VAL_006", "field.format.invalid", 400),
        DATE_FORMAT_INVALID("VAL_007", "date.format.invalid", 400),
        CURRENCY_INVALID("VAL_008", "currency.invalid", 400),
        IBAN_INVALID("VAL_009", "iban.invalid", 400),
        
        // System and Infrastructure Errors (5000-5999)
        INTERNAL_SERVER_ERROR("SYS_001", "internal.server.error", 500),
        DATABASE_ERROR("SYS_002", "database.error", 500),
        EXTERNAL_SERVICE_ERROR("SYS_003", "external.service.error", 502),
        RATE_LIMIT_EXCEEDED("SYS_004", "rate.limit.exceeded", 429),
        CIRCUIT_BREAKER_OPEN("SYS_005", "circuit.breaker.open", 503),
        SERVICE_UNAVAILABLE("SYS_006", "service.unavailable", 503),
        TIMEOUT_ERROR("SYS_007", "timeout.error", 408),
        REDIS_CONNECTION_ERROR("SYS_008", "redis.connection.error", 500),
        KAFKA_ERROR("SYS_009", "kafka.error", 500),
        
        // SAGA Pattern Errors (6000-6999)
        SAGA_EXECUTION_FAILED("SAGA_001", "saga.execution.failed", 500),
        SAGA_COMPENSATION_FAILED("SAGA_002", "saga.compensation.failed", 500),
        SAGA_TIMEOUT("SAGA_003", "saga.timeout", 408),
        SAGA_ROLLBACK_FAILED("SAGA_004", "saga.rollback.failed", 500),
        SAGA_STATE_INVALID("SAGA_005", "saga.state.invalid", 422),
        
        // Service Mesh and Network Errors (7000-7999)
        SERVICE_MESH_ERROR("MESH_001", "service.mesh.error", 500),
        MTLS_HANDSHAKE_FAILED("MESH_002", "mtls.handshake.failed", 500),
        ENVOY_PROXY_ERROR("MESH_003", "envoy.proxy.error", 502),
        LOAD_BALANCER_ERROR("MESH_004", "load.balancer.error", 502),
        NETWORK_TIMEOUT("MESH_005", "network.timeout", 408),
        
        // Compliance and Regulatory Errors (8000-8999)
        REGULATORY_COMPLIANCE_FAILED("REG_001", "regulatory.compliance.failed", 422),
        PSD2_COMPLIANCE_VIOLATION("REG_002", "psd2.compliance.violation", 422),
        GDPR_VIOLATION("REG_003", "gdpr.violation", 422),
        AUDIT_LOG_FAILED("REG_004", "audit.log.failed", 500),
        DATA_RETENTION_VIOLATION("REG_005", "data.retention.violation", 422),
        
        // Generic Errors (9000-9999)
        UNKNOWN_ERROR("GEN_001", "unknown.error", 500),
        FEATURE_NOT_IMPLEMENTED("GEN_002", "feature.not.implemented", 501),
        MAINTENANCE_MODE("GEN_003", "maintenance.mode", 503);

        private final String code;
        private final String messageKey;
        private final int httpStatus;
    }

    /**
     * Get localized error message
     */
    public String getMessage(ErrorCode errorCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(errorCode.getMessageKey(), args, errorCode.getMessageKey(), locale);
    }

    /**
     * Get localized error message with specific locale
     */
    public String getMessage(ErrorCode errorCode, Locale locale, Object... args) {
        return messageSource.getMessage(errorCode.getMessageKey(), args, errorCode.getMessageKey(), locale);
    }

    /**
     * Create a standardized error response
     */
    public BankingErrorResponse createErrorResponse(ErrorCode errorCode, Object... args) {
        return BankingErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(getMessage(errorCode, args))
                .httpStatus(errorCode.getHttpStatus())
                .timestamp(System.currentTimeMillis())
                .locale(LocaleContextHolder.getLocale().toString())
                .build();
    }

    /**
     * Create error response with additional details
     */
    public BankingErrorResponse createErrorResponse(ErrorCode errorCode, String details, Object... args) {
        return BankingErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(getMessage(errorCode, args))
                .details(details)
                .httpStatus(errorCode.getHttpStatus())
                .timestamp(System.currentTimeMillis())
                .locale(LocaleContextHolder.getLocale().toString())
                .build();
    }
}