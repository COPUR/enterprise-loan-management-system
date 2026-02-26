package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.PaymentWorkflowId;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of a batch payment workflow execution.
 * Contains aggregated results and individual payment outcomes.
 */
@Value
@Builder
public class BatchPaymentWorkflowResult {
    
    PaymentWorkflowId workflowId;
    String batchId;
    
    BatchStatus batchStatus;
    
    Instant startedAt;
    Instant completedAt;
    Long totalDurationMillis;
    
    // Summary statistics
    int totalPayments;
    int successfulPayments;
    int failedPayments;
    int skippedPayments;
    
    BigDecimal totalAmountProcessed;
    BigDecimal totalAmountFailed;
    String currency;
    
    // Individual results
    List<PaymentItemResult> itemResults;
    
    // Processing details
    String processingMode;
    boolean stoppedOnError;
    Integer maxConcurrency;
    
    // Error summary
    List<String> uniqueErrorCodes;
    Map<String, Integer> errorCodeCounts;
    
    // Performance metrics
    Long averageItemDurationMillis;
    Long minItemDurationMillis;
    Long maxItemDurationMillis;
    
    Map<String, Object> batchContext;
    List<String> warnings;
    
    /**
     * Creates a successful batch result.
     */
    public static BatchPaymentWorkflowResult success(
            PaymentWorkflowId workflowId,
            String batchId,
            List<PaymentItemResult> itemResults,
            Instant startedAt,
            Instant completedAt) {
        
        BatchStatistics stats = calculateStatistics(itemResults);
        
        return BatchPaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .batchId(batchId)
                .batchStatus(BatchStatus.COMPLETED)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .totalDurationMillis(completedAt.toEpochMilli() - startedAt.toEpochMilli())
                .totalPayments(stats.totalPayments)
                .successfulPayments(stats.successfulPayments)
                .failedPayments(stats.failedPayments)
                .skippedPayments(stats.skippedPayments)
                .totalAmountProcessed(stats.totalAmountProcessed)
                .totalAmountFailed(stats.totalAmountFailed)
                .itemResults(itemResults)
                .uniqueErrorCodes(stats.uniqueErrorCodes)
                .errorCodeCounts(stats.errorCodeCounts)
                .averageItemDurationMillis(stats.averageItemDurationMillis)
                .minItemDurationMillis(stats.minItemDurationMillis)
                .maxItemDurationMillis(stats.maxItemDurationMillis)
                .build();
    }
    
    /**
     * Creates a partial batch result (stopped on error).
     */
    public static BatchPaymentWorkflowResult partial(
            PaymentWorkflowId workflowId,
            String batchId,
            List<PaymentItemResult> itemResults,
            String stoppingError,
            Instant startedAt,
            Instant completedAt) {
        
        BatchStatistics stats = calculateStatistics(itemResults);
        
        return BatchPaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .batchId(batchId)
                .batchStatus(BatchStatus.PARTIALLY_COMPLETED)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .totalDurationMillis(completedAt.toEpochMilli() - startedAt.toEpochMilli())
                .totalPayments(stats.totalPayments)
                .successfulPayments(stats.successfulPayments)
                .failedPayments(stats.failedPayments)
                .skippedPayments(stats.skippedPayments)
                .totalAmountProcessed(stats.totalAmountProcessed)
                .totalAmountFailed(stats.totalAmountFailed)
                .itemResults(itemResults)
                .stoppedOnError(true)
                .uniqueErrorCodes(stats.uniqueErrorCodes)
                .errorCodeCounts(stats.errorCodeCounts)
                .warnings(List.of("Batch processing stopped due to error: " + stoppingError))
                .build();
    }
    
    /**
     * Creates a failed batch result.
     */
    public static BatchPaymentWorkflowResult failure(
            PaymentWorkflowId workflowId,
            String batchId,
            String errorCode,
            String errorMessage,
            Instant startedAt) {
        
        return BatchPaymentWorkflowResult.builder()
                .workflowId(workflowId)
                .batchId(batchId)
                .batchStatus(BatchStatus.FAILED)
                .startedAt(startedAt)
                .completedAt(Instant.now())
                .totalPayments(0)
                .successfulPayments(0)
                .failedPayments(0)
                .skippedPayments(0)
                .totalAmountProcessed(BigDecimal.ZERO)
                .totalAmountFailed(BigDecimal.ZERO)
                .itemResults(List.of())
                .uniqueErrorCodes(List.of(errorCode))
                .errorCodeCounts(Map.of(errorCode, 1))
                .warnings(List.of("Batch failed to start: " + errorMessage))
                .build();
    }
    
    /**
     * Calculates batch statistics from item results.
     */
    private static BatchStatistics calculateStatistics(List<PaymentItemResult> itemResults) {
        BatchStatistics stats = new BatchStatistics();
        
        stats.totalPayments = itemResults.size();
        stats.successfulPayments = (int) itemResults.stream()
                .filter(PaymentItemResult::isSuccess)
                .count();
        stats.failedPayments = (int) itemResults.stream()
                .filter(r -> !r.isSuccess() && !r.isSkipped())
                .count();
        stats.skippedPayments = (int) itemResults.stream()
                .filter(PaymentItemResult::isSkipped)
                .count();
        
        stats.totalAmountProcessed = itemResults.stream()
                .filter(PaymentItemResult::isSuccess)
                .map(PaymentItemResult::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.totalAmountFailed = itemResults.stream()
                .filter(r -> !r.isSuccess() && !r.isSkipped())
                .map(PaymentItemResult::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Error analysis
        Map<String, Long> errorCounts = itemResults.stream()
                .filter(r -> r.getErrorCode() != null)
                .collect(Collectors.groupingBy(
                        PaymentItemResult::getErrorCode,
                        Collectors.counting()));
        
        stats.uniqueErrorCodes = errorCounts.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        
        stats.errorCodeCounts = errorCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()));
        
        // Performance metrics
        List<Long> durations = itemResults.stream()
                .map(PaymentItemResult::getDurationMillis)
                .filter(d -> d != null && d > 0)
                .collect(Collectors.toList());
        
        if (!durations.isEmpty()) {
            stats.averageItemDurationMillis = durations.stream()
                    .mapToLong(Long::longValue)
                    .sum() / durations.size();
            stats.minItemDurationMillis = durations.stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0L);
            stats.maxItemDurationMillis = durations.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
        }
        
        return stats;
    }
    
    /**
     * Gets the success rate of the batch.
     */
    public double getSuccessRate() {
        if (totalPayments == 0) {
            return 0.0;
        }
        return (double) successfulPayments / totalPayments * 100;
    }
    
    /**
     * Checks if the batch was fully successful.
     */
    public boolean isFullySuccessful() {
        return batchStatus == BatchStatus.COMPLETED && 
               failedPayments == 0 && 
               successfulPayments == totalPayments;
    }
    
    /**
     * Gets a summary of the batch result.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Batch ").append(batchId)
               .append(" - Status: ").append(batchStatus)
               .append(" - Success: ").append(successfulPayments)
               .append("/").append(totalPayments)
               .append(" (").append(String.format("%.1f%%", getSuccessRate())).append(")")
               .append(" - Amount: ").append(totalAmountProcessed).append(" ").append(currency);
        
        if (failedPayments > 0) {
            summary.append(" - Failed: ").append(failedPayments)
                   .append(" (").append(totalAmountFailed).append(")");
        }
        
        if (totalDurationMillis != null) {
            summary.append(" - Duration: ").append(totalDurationMillis).append("ms");
        }
        
        return summary.toString();
    }
    
    /**
     * Individual payment item result.
     */
    @Value
    @Builder
    public static class PaymentItemResult {
        String paymentId;
        int sequenceNumber;
        
        boolean success;
        boolean skipped;
        
        BigDecimal amount;
        String currency;
        
        String transactionId;
        String confirmationNumber;
        
        String errorCode;
        String errorMessage;
        
        Instant startedAt;
        Instant completedAt;
        Long durationMillis;
        
        Map<String, String> metadata;
    }
    
    /**
     * Batch processing status.
     */
    public enum BatchStatus {
        PROCESSING,          // Batch is being processed
        COMPLETED,           // All items processed
        PARTIALLY_COMPLETED, // Some items processed, stopped early
        FAILED,              // Batch processing failed
        CANCELLED            // Batch was cancelled
    }
    
    /**
     * Internal statistics helper class.
     */
    private static class BatchStatistics {
        int totalPayments;
        int successfulPayments;
        int failedPayments;
        int skippedPayments;
        BigDecimal totalAmountProcessed = BigDecimal.ZERO;
        BigDecimal totalAmountFailed = BigDecimal.ZERO;
        List<String> uniqueErrorCodes;
        Map<String, Integer> errorCodeCounts;
        Long averageItemDurationMillis;
        Long minItemDurationMillis;
        Long maxItemDurationMillis;
    }
}