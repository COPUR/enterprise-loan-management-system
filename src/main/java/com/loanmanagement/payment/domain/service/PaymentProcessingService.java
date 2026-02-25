package com.loanmanagement.payment.domain.service;

import com.loanmanagement.loan.domain.model.LoanId;
import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Domain Service for Payment Processing
 * Handles the core business logic for payment processing
 */
@Slf4j
@Service
public class PaymentProcessingService {
    
    private final PaymentValidationService paymentValidationService;
    private final PaymentAllocationService paymentAllocationService;
    
    public PaymentProcessingService() {
        this.paymentValidationService = new PaymentValidationService();
        this.paymentAllocationService = new PaymentAllocationService();
    }

    /**
     * Process a payment request
     */
    public PaymentResult processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment request for loan: {}, amount: {}", 
                paymentRequest.getLoanId(), paymentRequest.getPaymentAmount());
        
        // Validate payment request
        PaymentValidationResult validationResult = paymentValidationService.validatePayment(paymentRequest);
        if (!validationResult.isValid()) {
            return createFailedPaymentResult(paymentRequest, validationResult);
        }
        
        try {
            // Create payment allocation
            LoanBalance currentBalance = getCurrentLoanBalance(paymentRequest.getLoanId());
            PaymentAllocation allocation = paymentAllocationService.allocatePayment(
                    paymentRequest.getPaymentAmount(), 
                    currentBalance, 
                    PaymentAllocationStrategy.STANDARD);
            
            // Generate transaction ID
            String transactionId = generateTransactionId();
            
            // Calculate processing fee
            Money processingFee = calculateProcessingFee(paymentRequest);
            
            // Determine if this is a partial payment
            boolean isPartialPayment = isPartialPayment(paymentRequest, currentBalance);
            Money remainingBalance = calculateRemainingBalance(paymentRequest, currentBalance);
            
            // Check for early payoff
            boolean isEarlyPayoff = paymentRequest.getPaymentType() == PaymentType.EARLY_PAYOFF;
            Money interestSavings = isEarlyPayoff ? calculateInterestSavings(paymentRequest) : null;
            
            return PaymentResult.builder()
                    .paymentId(PaymentId.generate())
                    .paymentStatus(PaymentStatus.PROCESSED)
                    .processedAmount(paymentRequest.getPaymentAmount())
                    .transactionId(transactionId)
                    .allocation(allocation)
                    .processingFee(processingFee)
                    .successful(true)
                    .partialPayment(isPartialPayment)
                    .remainingBalance(remainingBalance)
                    .earlyPayoff(isEarlyPayoff)
                    .interestSavings(interestSavings)
                    .processedDate(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing payment for loan: {}", paymentRequest.getLoanId(), e);
            return createFailedPaymentResult(paymentRequest, 
                    PaymentFailureReason.PROCESSING_ERROR, e.getMessage());
        }
    }
    
    /**
     * Process payment with pre-validation result
     */
    public PaymentResult processPaymentWithValidation(PaymentRequest paymentRequest, 
                                                     PaymentValidationResult validationResult) {
        if (!validationResult.isValid()) {
            return createFailedPaymentResult(paymentRequest, validationResult);
        }
        
        return processPayment(paymentRequest);
    }
    
    /**
     * Process a batch of payments
     */
    public PaymentBatchResult processBatchPayments(PaymentBatchRequest batchRequest) {
        log.info("Processing batch of {} payments", batchRequest.getPayments().size());
        
        List<PaymentResult> results = new ArrayList<>();
        int successfulPayments = 0;
        int failedPayments = 0;
        int throttledRequests = 0;
        Money totalProcessedAmount = Money.zero("USD");
        
        long startTime = System.currentTimeMillis();
        
        for (PaymentRequest paymentRequest : batchRequest.getPayments()) {
            try {
                // Apply rate limiting
                if (shouldThrottleRequest(batchRequest, results.size())) {
                    throttledRequests++;
                    continue;
                }
                
                PaymentResult result = processPayment(paymentRequest);
                results.add(result);
                
                if (result.isSuccessful()) {
                    successfulPayments++;
                    totalProcessedAmount = totalProcessedAmount.add(result.getProcessedAmount());
                } else {
                    failedPayments++;
                }
                
            } catch (Exception e) {
                log.error("Error processing payment in batch: {}", paymentRequest.getLoanId(), e);
                failedPayments++;
            }
        }
        
        long processingTimeMs = System.currentTimeMillis() - startTime;
        
        return PaymentBatchResult.builder()
                .batchId(batchRequest.getBatchId())
                .totalPayments(batchRequest.getPayments().size())
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .throttledRequests(throttledRequests)
                .totalProcessedAmount(totalProcessedAmount)
                .processingTimeMs(processingTimeMs)
                .allSuccessful(failedPayments == 0 && throttledRequests == 0)
                .results(results)
                .rateLimitingDetails(createRateLimitingDetails(throttledRequests))
                .build();
    }
    
    /**
     * Process payment reversal
     */
    public PaymentReversalResult processPaymentReversal(PaymentReversalRequest reversalRequest) {
        log.info("Processing payment reversal for payment: {}", reversalRequest.getOriginalPaymentId());
        
        try {
            String reversalTransactionId = generateTransactionId();
            
            return PaymentReversalResult.builder()
                    .reversalId(PaymentReversalId.generate())
                    .originalPaymentId(reversalRequest.getOriginalPaymentId())
                    .reversedAmount(reversalRequest.getReversalAmount())
                    .reversalTransactionId(reversalTransactionId)
                    .successful(true)
                    .processedDate(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing payment reversal: {}", reversalRequest.getOriginalPaymentId(), e);
            return PaymentReversalResult.builder()
                    .originalPaymentId(reversalRequest.getOriginalPaymentId())
                    .successful(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Calculate payment analytics
     */
    public PaymentAnalytics calculatePaymentAnalytics(LoanId loanId, List<Payment> paymentHistory) {
        if (paymentHistory.isEmpty()) {
            return PaymentAnalytics.builder()
                    .loanId(loanId)
                    .averagePaymentAmount(Money.zero("USD"))
                    .onTimePaymentPercentage(BigDecimal.ZERO)
                    .build();
        }
        
        Money totalPayments = paymentHistory.stream()
                .map(Payment::getPaymentAmount)
                .reduce(Money.zero("USD"), Money::add);
        
        Money averagePaymentAmount = totalPayments.divide(new BigDecimal(paymentHistory.size()));
        
        long onTimePayments = paymentHistory.stream()
                .filter(this::isOnTimePayment)
                .count();
        
        BigDecimal onTimePercentage = new BigDecimal(onTimePayments)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(paymentHistory.size()), 2, java.math.RoundingMode.HALF_UP);
        
        Payment largestPayment = paymentHistory.stream()
                .max((p1, p2) -> p1.getPaymentAmount().getAmount().compareTo(p2.getPaymentAmount().getAmount()))
                .orElse(null);
        
        Payment smallestPayment = paymentHistory.stream()
                .min((p1, p2) -> p1.getPaymentAmount().getAmount().compareTo(p2.getPaymentAmount().getAmount()))
                .orElse(null);
        
        return PaymentAnalytics.builder()
                .loanId(loanId)
                .paymentCount(paymentHistory.size())
                .totalAmount(totalPayments)
                .averagePaymentAmount(averagePaymentAmount)
                .onTimePaymentPercentage(onTimePercentage)
                .largestPayment(largestPayment)
                .smallestPayment(smallestPayment)
                .paymentTrends(calculatePaymentTrends(paymentHistory))
                .build();
    }
    
    /**
     * Predict next payment
     */
    public PaymentPrediction predictNextPayment(LoanId loanId, List<Payment> paymentHistory) {
        if (paymentHistory.isEmpty()) {
            return PaymentPrediction.builder()
                    .loanId(loanId)
                    .confidenceScore(BigDecimal.ZERO)
                    .build();
        }
        
        // Simple prediction based on payment patterns
        Money averageAmount = calculateAveragePaymentAmount(paymentHistory);
        LocalDateTime predictedDate = predictNextPaymentDate(paymentHistory);
        BigDecimal confidenceScore = calculatePredictionConfidence(paymentHistory);
        
        return PaymentPrediction.builder()
                .loanId(loanId)
                .predictedPaymentDate(predictedDate)
                .predictedPaymentAmount(averageAmount)
                .confidenceScore(confidenceScore)
                .basedOnPayments(paymentHistory.size())
                .build();
    }
    
    /**
     * Detect payment anomalies
     */
    public PaymentAnomalyAnalysis detectPaymentAnomalies(LoanId loanId, List<Payment> paymentHistory, 
                                                        Payment newPayment) {
        List<PaymentAnomaly> anomalies = new ArrayList<>();
        
        // Check for unusual amount
        if (isUnusualAmount(newPayment, paymentHistory)) {
            anomalies.add(PaymentAnomaly.builder()
                    .anomalyType(PaymentAnomalyType.UNUSUAL_AMOUNT)
                    .description("Payment amount significantly different from historical average")
                    .severity(AnomalySeverity.MEDIUM)
                    .build());
        }
        
        // Check for unusual timing
        if (isUnusualTiming(newPayment, paymentHistory)) {
            anomalies.add(PaymentAnomaly.builder()
                    .anomalyType(PaymentAnomalyType.UNUSUAL_TIMING)
                    .description("Payment timing differs from established pattern")
                    .severity(AnomalySeverity.LOW)
                    .build());
        }
        
        return PaymentAnomalyAnalysis.builder()
                .loanId(loanId)
                .analyzedPayment(newPayment)
                .anomalies(anomalies)
                .hasAnomalies(!anomalies.isEmpty())
                .analysisDate(LocalDateTime.now())
                .build();
    }
    
    // Private helper methods
    
    private PaymentResult createFailedPaymentResult(PaymentRequest paymentRequest, 
                                                   PaymentValidationResult validationResult) {
        PaymentFailureReason failureReason = determineFailureReason(validationResult);
        String failureMessage = validationResult.getViolations().stream()
                .findFirst()
                .map(PaymentViolation::getMessage)
                .orElse("Payment validation failed");
        
        return createFailedPaymentResult(paymentRequest, failureReason, failureMessage);
    }
    
    private PaymentResult createFailedPaymentResult(PaymentRequest paymentRequest, 
                                                   PaymentFailureReason failureReason, 
                                                   String failureMessage) {
        return PaymentResult.builder()
                .paymentId(PaymentId.generate())
                .paymentStatus(PaymentStatus.FAILED)
                .processedAmount(Money.zero(paymentRequest.getPaymentAmount().getCurrency()))
                .successful(false)
                .failureReason(failureReason)
                .failureMessage(failureMessage)
                .processedDate(LocalDateTime.now())
                .build();
    }
    
    private LoanBalance getCurrentLoanBalance(LoanId loanId) {
        // In a real implementation, this would fetch from the loan service
        return LoanBalance.builder()
                .loanId(loanId)
                .principalBalance(Money.of("USD", new BigDecimal("95000.00")))
                .interestBalance(Money.of("USD", new BigDecimal("450.00")))
                .feesBalance(Money.of("USD", new BigDecimal("25.00")))
                .totalBalance(Money.of("USD", new BigDecimal("95475.00")))
                .build();
    }
    
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0x10000));
    }
    
    private Money calculateProcessingFee(PaymentRequest paymentRequest) {
        // Fee calculation based on payment method
        BigDecimal feeRate = switch (paymentRequest.getPaymentSource().getSourceType()) {
            case CREDIT_CARD -> new BigDecimal("0.029"); // 2.9%
            case DEBIT_CARD -> new BigDecimal("0.015"); // 1.5%
            case BANK_ACCOUNT -> new BigDecimal("0.001"); // 0.1%
            case WIRE_TRANSFER -> new BigDecimal("25.00"); // Flat fee
            default -> BigDecimal.ZERO;
        };
        
        if (paymentRequest.getPaymentSource().getSourceType() == PaymentSourceType.WIRE_TRANSFER) {
            return Money.of(paymentRequest.getPaymentAmount().getCurrency(), feeRate);
        } else {
            return paymentRequest.getPaymentAmount().multiply(feeRate);
        }
    }
    
    private boolean isPartialPayment(PaymentRequest paymentRequest, LoanBalance currentBalance) {
        return paymentRequest.getPaymentAmount().getAmount()
                .compareTo(currentBalance.getTotalBalance().getAmount()) < 0;
    }
    
    private Money calculateRemainingBalance(PaymentRequest paymentRequest, LoanBalance currentBalance) {
        return currentBalance.getTotalBalance().subtract(paymentRequest.getPaymentAmount());
    }
    
    private Money calculateInterestSavings(PaymentRequest paymentRequest) {
        // Simplified interest savings calculation
        return paymentRequest.getPaymentAmount().multiply(new BigDecimal("0.05"));
    }
    
    private PaymentFailureReason determineFailureReason(PaymentValidationResult validationResult) {
        return validationResult.getViolations().stream()
                .findFirst()
                .map(violation -> switch (violation.getViolationType()) {
                    case INSUFFICIENT_FUNDS -> PaymentFailureReason.INSUFFICIENT_FUNDS;
                    case INVALID_PAYMENT_SOURCE -> PaymentFailureReason.INVALID_PAYMENT_METHOD;
                    case AMOUNT_EXCEEDS_LIMIT -> PaymentFailureReason.AMOUNT_LIMIT_EXCEEDED;
                    case DUPLICATE_PAYMENT -> PaymentFailureReason.DUPLICATE_TRANSACTION;
                    default -> PaymentFailureReason.VALIDATION_ERROR;
                })
                .orElse(PaymentFailureReason.UNKNOWN_ERROR);
    }
    
    private boolean shouldThrottleRequest(PaymentBatchRequest batchRequest, int processedCount) {
        // Simple rate limiting: maximum 10 requests per batch
        return processedCount >= 10;
    }
    
    private String createRateLimitingDetails(int throttledRequests) {
        return String.format("Rate limiting applied: %d requests throttled", throttledRequests);
    }
    
    private boolean isOnTimePayment(Payment payment) {
        // Simplified: assume payment is on time if processed
        return payment.isSuccessful();
    }
    
    private List<PaymentTrend> calculatePaymentTrends(List<Payment> paymentHistory) {
        // Simplified trend calculation
        return List.of(PaymentTrend.builder()
                .period("MONTHLY")
                .averageAmount(calculateAveragePaymentAmount(paymentHistory))
                .paymentCount(paymentHistory.size())
                .build());
    }
    
    private Money calculateAveragePaymentAmount(List<Payment> paymentHistory) {
        if (paymentHistory.isEmpty()) {
            return Money.zero("USD");
        }
        
        Money total = paymentHistory.stream()
                .map(Payment::getPaymentAmount)
                .reduce(Money.zero("USD"), Money::add);
        
        return total.divide(new BigDecimal(paymentHistory.size()));
    }
    
    private LocalDateTime predictNextPaymentDate(List<Payment> paymentHistory) {
        // Simple prediction: add 30 days to last payment
        return paymentHistory.stream()
                .max((p1, p2) -> p1.getPaymentDate().compareTo(p2.getPaymentDate()))
                .map(payment -> payment.getPaymentDate().plusDays(30))
                .orElse(LocalDateTime.now().plusDays(30));
    }
    
    private BigDecimal calculatePredictionConfidence(List<Payment> paymentHistory) {
        // Simple confidence calculation based on payment regularity
        if (paymentHistory.size() < 3) {
            return new BigDecimal("50.0");
        } else if (paymentHistory.size() < 6) {
            return new BigDecimal("75.0");
        } else {
            return new BigDecimal("90.0");
        }
    }
    
    private boolean isUnusualAmount(Payment newPayment, List<Payment> paymentHistory) {
        if (paymentHistory.isEmpty()) {
            return false;
        }
        
        Money averageAmount = calculateAveragePaymentAmount(paymentHistory);
        Money threshold = averageAmount.multiply(new BigDecimal("2.0")); // 200% of average
        
        return newPayment.getPaymentAmount().getAmount().compareTo(threshold.getAmount()) > 0;
    }
    
    private boolean isUnusualTiming(Payment newPayment, List<Payment> paymentHistory) {
        // Simplified: check if payment is on weekend
        int dayOfWeek = newPayment.getPaymentDate().getDayOfWeek().getValue();
        return dayOfWeek > 5; // Saturday or Sunday
    }
}