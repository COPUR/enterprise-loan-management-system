package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowPriority;
import com.loanmanagement.payment.domain.model.PaymentWorkflowType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request to start a recurring payment workflow.
 * Extends the base workflow request with recurring-specific fields.
 */
@Value
@Builder
public class RecurringPaymentWorkflowRequest {
    
    // Base workflow fields
    String paymentId;
    String customerId;
    String accountId;
    
    BigDecimal amount;
    String currency;
    
    String sourceAccountId;
    String destinationAccountId;
    
    // Recurring-specific fields
    RecurrencePattern recurrencePattern;
    LocalDate startDate;
    LocalDate endDate;
    
    Integer totalOccurrences;
    Integer remainingOccurrences;
    
    LocalTime preferredExecutionTime;
    String timezone;
    
    DayOfMonthStrategy dayOfMonthStrategy;
    List<Integer> skipDates;
    
    boolean autoRenew;
    Integer renewalOccurrences;
    
    BigDecimal minimumBalance;
    boolean pauseOnInsufficientFunds;
    
    String recurringPaymentReference;
    String mandateReference;
    
    Map<String, Object> workflowContext;
    Map<String, String> metadata;
    
    String requestedBy;
    
    /**
     * Validates the recurring payment request.
     */
    public void validate() {
        // Validate base fields
        Objects.requireNonNull(paymentId, "Payment ID is required");
        Objects.requireNonNull(customerId, "Customer ID is required");
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        Objects.requireNonNull(sourceAccountId, "Source account is required");
        Objects.requireNonNull(destinationAccountId, "Destination account is required");
        
        // Validate recurring fields
        Objects.requireNonNull(recurrencePattern, "Recurrence pattern is required");
        Objects.requireNonNull(startDate, "Start date is required");
        Objects.requireNonNull(requestedBy, "Requester is required");
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        if (totalOccurrences != null && totalOccurrences <= 0) {
            throw new IllegalArgumentException("Total occurrences must be positive");
        }
        
        validateRecurrencePattern();
    }
    
    /**
     * Validates the recurrence pattern.
     */
    private void validateRecurrencePattern() {
        switch (recurrencePattern) {
            case DAILY:
                // No additional validation needed
                break;
                
            case WEEKLY:
            case BIWEEKLY:
                // Could validate day of week if provided
                break;
                
            case MONTHLY:
            case QUARTERLY:
            case SEMI_ANNUAL:
            case ANNUAL:
                if (dayOfMonthStrategy == null) {
                    throw new IllegalArgumentException(
                            "Day of month strategy required for " + recurrencePattern);
                }
                break;
                
            case CUSTOM:
                if (workflowContext == null || 
                    !workflowContext.containsKey("customPattern")) {
                    throw new IllegalArgumentException(
                            "Custom pattern details required in workflow context");
                }
                break;
        }
    }
    
    /**
     * Converts to a base payment workflow request.
     */
    public PaymentWorkflowRequest toWorkflowRequest() {
        return PaymentWorkflowRequest.builder()
                .paymentId(paymentId)
                .customerId(customerId)
                .accountId(accountId)
                .workflowType(PaymentWorkflowType.RECURRING_PAYMENT)
                .priority(PaymentWorkflowPriority.MEDIUM)
                .amount(amount)
                .currency(currency)
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .paymentReference(recurringPaymentReference)
                .requestedAt(java.time.Instant.now())
                .requestedBy(requestedBy)
                .workflowContext(buildWorkflowContext())
                .metadata(metadata)
                .requiresValidation(true)
                .maxRetries(3)
                .timeoutMillis(600000L) // 10 minutes
                .build();
    }
    
    /**
     * Builds the workflow context with recurring-specific data.
     */
    private Map<String, Object> buildWorkflowContext() {
        Map<String, Object> context = new java.util.HashMap<>();
        
        if (workflowContext != null) {
            context.putAll(workflowContext);
        }
        
        context.put("recurrencePattern", recurrencePattern);
        context.put("startDate", startDate.toString());
        
        if (endDate != null) {
            context.put("endDate", endDate.toString());
        }
        
        if (totalOccurrences != null) {
            context.put("totalOccurrences", totalOccurrences);
        }
        
        if (preferredExecutionTime != null) {
            context.put("preferredExecutionTime", preferredExecutionTime.toString());
        }
        
        if (timezone != null) {
            context.put("timezone", timezone);
        }
        
        context.put("autoRenew", autoRenew);
        context.put("pauseOnInsufficientFunds", pauseOnInsufficientFunds);
        
        if (mandateReference != null) {
            context.put("mandateReference", mandateReference);
        }
        
        return context;
    }
    
    /**
     * Calculates the next execution date.
     */
    public LocalDate calculateNextExecutionDate(LocalDate fromDate) {
        LocalDate nextDate = fromDate;
        
        switch (recurrencePattern) {
            case DAILY:
                nextDate = fromDate.plusDays(1);
                break;
            case WEEKLY:
                nextDate = fromDate.plusWeeks(1);
                break;
            case BIWEEKLY:
                nextDate = fromDate.plusWeeks(2);
                break;
            case MONTHLY:
                nextDate = fromDate.plusMonths(1);
                nextDate = adjustForDayOfMonth(nextDate);
                break;
            case QUARTERLY:
                nextDate = fromDate.plusMonths(3);
                nextDate = adjustForDayOfMonth(nextDate);
                break;
            case SEMI_ANNUAL:
                nextDate = fromDate.plusMonths(6);
                nextDate = adjustForDayOfMonth(nextDate);
                break;
            case ANNUAL:
                nextDate = fromDate.plusYears(1);
                nextDate = adjustForDayOfMonth(nextDate);
                break;
            case CUSTOM:
                // Custom logic would be in workflow context
                break;
        }
        
        // Skip dates if specified
        while (skipDates != null && skipDates.contains(nextDate.getDayOfMonth())) {
            nextDate = nextDate.plusDays(1);
        }
        
        return nextDate;
    }
    
    /**
     * Adjusts date based on day of month strategy.
     */
    private LocalDate adjustForDayOfMonth(LocalDate date) {
        if (dayOfMonthStrategy == null) {
            return date;
        }
        
        int originalDay = startDate.getDayOfMonth();
        int lastDayOfMonth = date.lengthOfMonth();
        
        switch (dayOfMonthStrategy) {
            case FIXED_DAY:
                if (originalDay > lastDayOfMonth) {
                    return date.withDayOfMonth(lastDayOfMonth);
                }
                return date.withDayOfMonth(originalDay);
                
            case LAST_DAY:
                return date.withDayOfMonth(lastDayOfMonth);
                
            case LAST_BUSINESS_DAY:
                LocalDate lastDay = date.withDayOfMonth(lastDayOfMonth);
                while (isWeekend(lastDay)) {
                    lastDay = lastDay.minusDays(1);
                }
                return lastDay;
                
            default:
                return date;
        }
    }
    
    /**
     * Checks if a date is a weekend.
     */
    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6; // Saturday or Sunday
    }
    
    /**
     * Checks if the recurring payment should continue.
     */
    public boolean shouldContinue(int executedOccurrences) {
        if (endDate != null && LocalDate.now().isAfter(endDate)) {
            return false;
        }
        
        if (totalOccurrences != null && executedOccurrences >= totalOccurrences) {
            return autoRenew;
        }
        
        return true;
    }
    
    /**
     * Recurrence patterns.
     */
    public enum RecurrencePattern {
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY,
        QUARTERLY,
        SEMI_ANNUAL,
        ANNUAL,
        CUSTOM
    }
    
    /**
     * Day of month strategies.
     */
    public enum DayOfMonthStrategy {
        FIXED_DAY,          // Same day each month
        LAST_DAY,           // Last day of month
        LAST_BUSINESS_DAY,  // Last business day of month
        ADJUST_TO_VALID     // Adjust to last valid day if original doesn't exist
    }
}