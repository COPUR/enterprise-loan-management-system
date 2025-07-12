package com.amanahfi.platform.shared.exception;

import com.amanahfi.platform.shared.i18n.MessageService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.List;
import java.util.Map;

/**
 * Exception for data validation errors
 */
@Getter
@Configurable
public class ValidationException extends AmanahFiException {
    
    @Autowired
    private transient MessageService messageService;
    
    private final List<ValidationError> validationErrors;
    private final String fieldName;
    private final Object invalidValue;
    private final Object[] messageParams;
    
    public ValidationException(
            String errorCode,
            String fieldName,
            Object invalidValue,
            String userMessage,
            String technicalMessage,
            List<ValidationError> validationErrors,
            Map<String, Object> errorContext,
            Object... messageParams) {
        
        super(errorCode, userMessage, technicalMessage, ErrorSeverity.LOW, ErrorCategory.VALIDATION, errorContext);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
        this.validationErrors = validationErrors != null ? List.copyOf(validationErrors) : List.of();
        this.messageParams = messageParams != null ? messageParams.clone() : new Object[0];
    }
    
    public ValidationException(
            String errorCode,
            String fieldName,
            Object invalidValue,
            String userMessage,
            String technicalMessage,
            List<ValidationError> validationErrors,
            Object... messageParams) {
        
        this(errorCode, fieldName, invalidValue, userMessage, technicalMessage, validationErrors, Map.of(), messageParams);
    }
    
    public ValidationException(
            String errorCode,
            String fieldName,
            Object invalidValue,
            String userMessage,
            String technicalMessage,
            Object... messageParams) {
        
        this(errorCode, fieldName, invalidValue, userMessage, technicalMessage, List.of(), messageParams);
    }
    
    @Override
    public String getLocalizedMessage(String languageCode) {
        if (messageService == null) {
            return getUserMessage(); // Fallback if DI not available
        }
        
        try {
            // Try to get localized message using error code as key
            if (messageService.hasMessage(getErrorCode(), languageCode)) {
                return messageService.getMessage(getErrorCode(), languageCode, messageParams);
            }
            
            // Fallback to original user message
            return getUserMessage();
            
        } catch (Exception e) {
            return getUserMessage(); // Fallback on any error
        }
    }
    
    /**
     * Get all validation error messages localized
     */
    public List<String> getLocalizedValidationMessages(String languageCode) {
        if (messageService == null) {
            return validationErrors.stream()
                .map(ValidationError::message)
                .toList();
        }
        
        return validationErrors.stream()
            .map(error -> {
                try {
                    if (messageService.hasMessage(error.code(), languageCode)) {
                        return messageService.getMessage(error.code(), languageCode, error.params());
                    }
                    return error.message();
                } catch (Exception e) {
                    return error.message();
                }
            })
            .toList();
    }
    
    // Common validation exceptions factory methods
    
    public static ValidationException requiredFieldMissing(String fieldName) {
        return new ValidationException(
            "FIELD_REQUIRED",
            fieldName,
            null,
            "Field '" + fieldName + "' is required",
            "Required field missing: " + fieldName,
            List.of(new ValidationError("FIELD_REQUIRED", fieldName, "Field is required", new Object[]{fieldName})),
            fieldName
        );
    }
    
    public static ValidationException invalidFieldValue(String fieldName, Object value, String expectedFormat) {
        return new ValidationException(
            "FIELD_INVALID_VALUE",
            fieldName,
            value,
            "Invalid value for field '" + fieldName + "'",
            "Invalid value '" + value + "' for field '" + fieldName + "', expected: " + expectedFormat,
            List.of(new ValidationError("FIELD_INVALID_VALUE", fieldName, "Invalid field value", new Object[]{fieldName, value, expectedFormat})),
            fieldName, value, expectedFormat
        );
    }
    
    public static ValidationException fieldOutOfRange(String fieldName, Object value, Object minValue, Object maxValue) {
        return new ValidationException(
            "FIELD_OUT_OF_RANGE",
            fieldName,
            value,
            "Field '" + fieldName + "' value is out of allowed range",
            "Field '" + fieldName + "' value '" + value + "' is out of range [" + minValue + ", " + maxValue + "]",
            List.of(new ValidationError("FIELD_OUT_OF_RANGE", fieldName, "Field value out of range", new Object[]{fieldName, value, minValue, maxValue})),
            fieldName, value, minValue, maxValue
        );
    }
    
    public static ValidationException invalidEmailFormat(String fieldName, String email) {
        return new ValidationException(
            "INVALID_EMAIL_FORMAT",
            fieldName,
            email,
            "Invalid email format",
            "Invalid email format for field '" + fieldName + "': " + email,
            List.of(new ValidationError("INVALID_EMAIL_FORMAT", fieldName, "Invalid email format", new Object[]{email})),
            fieldName, email
        );
    }
    
    public static ValidationException invalidPhoneNumber(String fieldName, String phoneNumber) {
        return new ValidationException(
            "INVALID_PHONE_NUMBER",
            fieldName,
            phoneNumber,
            "Invalid phone number format",
            "Invalid phone number format for field '" + fieldName + "': " + phoneNumber,
            List.of(new ValidationError("INVALID_PHONE_NUMBER", fieldName, "Invalid phone number format", new Object[]{phoneNumber})),
            fieldName, phoneNumber
        );
    }
    
    public static ValidationException invalidDateFormat(String fieldName, String date) {
        return new ValidationException(
            "INVALID_DATE_FORMAT",
            fieldName,
            date,
            "Invalid date format",
            "Invalid date format for field '" + fieldName + "': " + date,
            List.of(new ValidationError("INVALID_DATE_FORMAT", fieldName, "Invalid date format", new Object[]{date})),
            fieldName, date
        );
    }
    
    public static ValidationException invalidCurrency(String fieldName, String currency) {
        return new ValidationException(
            "INVALID_CURRENCY",
            fieldName,
            currency,
            "Invalid currency code",
            "Invalid currency code for field '" + fieldName + "': " + currency,
            List.of(new ValidationError("INVALID_CURRENCY", fieldName, "Invalid currency code", new Object[]{currency})),
            fieldName, currency
        );
    }
    
    public static ValidationException multipleFieldErrors(List<ValidationError> errors) {
        StringBuilder userMessage = new StringBuilder("Validation failed for multiple fields:");
        StringBuilder technicalMessage = new StringBuilder("Multiple validation errors: ");
        
        for (ValidationError error : errors) {
            userMessage.append(" ").append(error.fieldName()).append(",");
            technicalMessage.append(error.fieldName()).append("=").append(error.message()).append("; ");
        }
        
        return new ValidationException(
            "MULTIPLE_VALIDATION_ERRORS",
            "multiple",
            null,
            userMessage.toString(),
            technicalMessage.toString(),
            errors,
            Map.of("errorCount", errors.size())
        );
    }
    
    public static ValidationException islamicFinanceConstraint(String fieldName, Object value, String constraint) {
        return new ValidationException(
            "ISLAMIC_FINANCE_CONSTRAINT",
            fieldName,
            value,
            "Value violates Islamic finance constraints",
            "Field '" + fieldName + "' value '" + value + "' violates Islamic finance constraint: " + constraint,
            List.of(new ValidationError("ISLAMIC_FINANCE_CONSTRAINT", fieldName, "Islamic finance constraint violation", new Object[]{fieldName, value, constraint})),
            Map.of("category", "SHARIA_COMPLIANCE"),
            fieldName, value, constraint
        );
    }
    
    public static ValidationException cbdcValidationFailed(String fieldName, Object value, String reason) {
        return new ValidationException(
            "CBDC_VALIDATION_FAILED",
            fieldName,
            value,
            "CBDC validation failed",
            "CBDC validation failed for field '" + fieldName + "' with value '" + value + "': " + reason,
            List.of(new ValidationError("CBDC_VALIDATION_FAILED", fieldName, "CBDC validation failed", new Object[]{fieldName, value, reason})),
            Map.of("category", "CBDC_COMPLIANCE"),
            fieldName, value, reason
        );
    }
    
    /**
     * Validation error record
     */
    public record ValidationError(
        String code,
        String fieldName,
        String message,
        Object[] params
    ) {
        public ValidationError {
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Validation error code cannot be null or empty");
            }
            if (fieldName == null || fieldName.trim().isEmpty()) {
                throw new IllegalArgumentException("Field name cannot be null or empty");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Validation error message cannot be null or empty");
            }
            if (params == null) {
                params = new Object[0];
            }
        }
    }
}