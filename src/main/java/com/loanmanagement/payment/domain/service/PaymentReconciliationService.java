package com.loanmanagement.payment.domain.service;

import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain Service for Payment Reconciliation
 * Handles reconciliation between scheduled and actual payments
 */
@Slf4j
@Service
public class PaymentReconciliationService {

    /**
     * Reconcile scheduled payments against actual payments
     */
    public PaymentReconciliationReport reconcilePayments(PaymentSchedule schedule, 
                                                        List<Payment> actualPayments, 
                                                        LocalDate reconciliationDate) {
        log.info("Reconciling payments for schedule: {} as of {}", schedule.getScheduleId(), reconciliationDate);
        
        List<ScheduledPayment> scheduledPayments = getScheduledPaymentsUpTo(schedule, reconciliationDate);
        List<PaymentVariance> variances = calculatePaymentVariances(scheduledPayments, actualPayments);
        
        ReconciliationSummary summary = calculateReconciliationSummary(scheduledPayments, actualPayments, variances);
        
        return PaymentReconciliationReport.builder()
                .reportId(ReconciliationReportId.generate())
                .scheduleId(schedule.getScheduleId())
                .reconciliationDate(reconciliationDate)
                .totalScheduledPayments(scheduledPayments.size())
                .totalActualPayments(actualPayments.size())
                .variances(variances)
                .summary(summary)
                .reconciliationAccuracy(calculateReconciliationAccuracy(variances))
                .generatedDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Identify discrepancies between scheduled and actual payments
     */
    public List<PaymentDiscrepancy> identifyDiscrepancies(PaymentSchedule schedule, 
                                                         List<Payment> actualPayments) {
        log.info("Identifying payment discrepancies for schedule: {}", schedule.getScheduleId());
        
        List<PaymentDiscrepancy> discrepancies = new ArrayList<>();
        
        // Group actual payments by loan for easier comparison
        Map<String, List<Payment>> paymentsByLoan = actualPayments.stream()
                .collect(Collectors.groupingBy(p -> p.getLoanId().toString()));
        
        for (ScheduledPayment scheduledPayment : schedule.getScheduledPayments()) {
            if (scheduledPayment.getStatus() != PaymentStatus.SCHEDULED) {
                continue; // Skip non-scheduled payments
            }
            
            String loanIdKey = scheduledPayment.getLoanId().toString();
            List<Payment> loanPayments = paymentsByLoan.getOrDefault(loanIdKey, List.of());
            
            // Find matching actual payment
            Payment matchingPayment = findMatchingPayment(scheduledPayment, loanPayments);
            
            if (matchingPayment == null) {
                // Missing payment
                discrepancies.add(createMissingPaymentDiscrepancy(scheduledPayment));
            } else {
                // Check for amount discrepancies
                if (!matchingPayment.getPaymentAmount().equals(scheduledPayment.getPaymentAmount())) {
                    discrepancies.add(createAmountDiscrepancy(scheduledPayment, matchingPayment));
                }
                
                // Check for date discrepancies
                LocalDate scheduledDate = scheduledPayment.getDueDate();
                LocalDate actualDate = matchingPayment.getPaymentDate().toLocalDate();
                if (!actualDate.equals(scheduledDate) && Math.abs(scheduledDate.until(actualDate, java.time.temporal.ChronoUnit.DAYS)) > 1) {
                    discrepancies.add(createDateDiscrepancy(scheduledPayment, matchingPayment));
                }
            }
        }
        
        // Check for unexpected payments
        for (Payment actualPayment : actualPayments) {
            ScheduledPayment matchingScheduled = findMatchingScheduledPayment(actualPayment, schedule.getScheduledPayments());
            if (matchingScheduled == null) {
                discrepancies.add(createUnexpectedPaymentDiscrepancy(actualPayment));
            }
        }
        
        return discrepancies;
    }
    
    /**
     * Reconcile with automatic resolution for minor discrepancies
     */
    public PaymentReconciliationResult reconcileWithAutoResolution(PaymentSchedule schedule, 
                                                                  List<Payment> actualPayments, 
                                                                  PaymentReconciliationConfig config) {
        log.info("Reconciling with auto-resolution for schedule: {}", schedule.getScheduleId());
        
        List<PaymentDiscrepancy> allDiscrepancies = identifyDiscrepancies(schedule, actualPayments);
        List<PaymentDiscrepancy> autoResolved = new ArrayList<>();
        List<PaymentDiscrepancy> manualReviewRequired = new ArrayList<>();
        
        for (PaymentDiscrepancy discrepancy : allDiscrepancies) {
            if (canAutoResolve(discrepancy, config)) {
                PaymentDiscrepancy resolved = autoResolveDiscrepancy(discrepancy, config);
                autoResolved.add(resolved);
                log.info("Auto-resolved discrepancy: {}", discrepancy.getDiscrepancyId());
            } else {
                manualReviewRequired.add(discrepancy);
            }
        }
        
        return PaymentReconciliationResult.builder()
                .reconciliationId(ReconciliationId.generate())
                .scheduleId(schedule.getScheduleId())
                .totalDiscrepancies(allDiscrepancies.size())
                .autoResolvedDiscrepancies(autoResolved.size())
                .manualReviewRequired(manualReviewRequired)
                .autoResolutionRate(calculateAutoResolutionRate(autoResolved.size(), allDiscrepancies.size()))
                .processedDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Analyze payment variances over a period
     */
    public PaymentVarianceAnalysis analyzePaymentVariances(PaymentSchedule schedule, 
                                                          List<Payment> actualPayments, 
                                                          LocalDate startDate, 
                                                          LocalDate endDate) {
        log.info("Analyzing payment variances from {} to {}", startDate, endDate);
        
        List<ScheduledPayment> periodScheduled = schedule.getScheduledPayments().stream()
                .filter(p -> !p.getDueDate().isBefore(startDate) && !p.getDueDate().isAfter(endDate))
                .toList();
        
        List<Payment> periodActual = actualPayments.stream()
                .filter(p -> !p.getPaymentDate().toLocalDate().isBefore(startDate) && 
                           !p.getPaymentDate().toLocalDate().isAfter(endDate))
                .toList();
        
        List<PaymentVariance> variances = calculatePaymentVariances(periodScheduled, periodActual);
        
        PaymentVarianceStatistics statistics = calculateVarianceStatistics(variances);
        Map<LocalDate, PaymentVariance> variancesByMonth = groupVariancesByMonth(variances);
        VarianceTrend trend = calculateVarianceTrend(variances);
        
        return PaymentVarianceAnalysis.builder()
                .analysisId(VarianceAnalysisId.generate())
                .scheduleId(schedule.getScheduleId())
                .analysisStartDate(startDate)
                .analysisEndDate(endDate)
                .variances(variances)
                .statistics(statistics)
                .variancesByMonth(variancesByMonth)
                .varianceTrend(trend)
                .averageVariance(statistics.getAverageVariance())
                .maxVariance(statistics.getMaxVariance())
                .minVariance(statistics.getMinVariance())
                .analysisDate(LocalDateTime.now())
                .build();
    }
    
    // Private helper methods
    
    private List<ScheduledPayment> getScheduledPaymentsUpTo(PaymentSchedule schedule, LocalDate cutoffDate) {
        return schedule.getScheduledPayments().stream()
                .filter(payment -> !payment.getDueDate().isAfter(cutoffDate))
                .toList();
    }
    
    private List<PaymentVariance> calculatePaymentVariances(List<ScheduledPayment> scheduledPayments, 
                                                           List<Payment> actualPayments) {
        List<PaymentVariance> variances = new ArrayList<>();
        
        for (ScheduledPayment scheduled : scheduledPayments) {
            Payment actual = findMatchingPayment(scheduled, actualPayments);
            
            if (actual != null) {
                Money amountVariance = actual.getPaymentAmount().subtract(scheduled.getPaymentAmount());
                int dayVariance = (int) scheduled.getDueDate().until(actual.getPaymentDate().toLocalDate(), 
                        java.time.temporal.ChronoUnit.DAYS);
                
                PaymentVariance variance = PaymentVariance.builder()
                        .varianceId(VarianceId.generate())
                        .scheduledPaymentId(scheduled.getPaymentId())
                        .actualPaymentId(actual.getPaymentId())
                        .scheduledAmount(scheduled.getPaymentAmount())
                        .actualAmount(actual.getPaymentAmount())
                        .amountVariance(amountVariance)
                        .scheduledDate(scheduled.getDueDate())
                        .actualDate(actual.getPaymentDate().toLocalDate())
                        .dayVariance(dayVariance)
                        .varianceType(determineVarianceType(amountVariance, dayVariance))
                        .build();
                
                variances.add(variance);
            } else {
                // Missing payment variance
                PaymentVariance missingVariance = PaymentVariance.builder()
                        .varianceId(VarianceId.generate())
                        .scheduledPaymentId(scheduled.getPaymentId())
                        .scheduledAmount(scheduled.getPaymentAmount())
                        .actualAmount(Money.zero(scheduled.getPaymentAmount().getCurrency()))
                        .amountVariance(scheduled.getPaymentAmount().negate())
                        .scheduledDate(scheduled.getDueDate())
                        .varianceType(PaymentVarianceType.MISSING_PAYMENT)
                        .build();
                
                variances.add(missingVariance);
            }
        }
        
        return variances;
    }
    
    private ReconciliationSummary calculateReconciliationSummary(List<ScheduledPayment> scheduledPayments, 
                                                               List<Payment> actualPayments, 
                                                               List<PaymentVariance> variances) {
        Money totalScheduled = scheduledPayments.stream()
                .map(ScheduledPayment::getPaymentAmount)
                .reduce(Money.zero("USD"), Money::add);
        
        Money totalActual = actualPayments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(Money.zero("USD"), Money::add);
        
        Money totalVariance = totalActual.subtract(totalScheduled);
        
        long exactMatches = variances.stream()
                .filter(v -> v.getVarianceType() == PaymentVarianceType.EXACT_MATCH)
                .count();
        
        double matchRate = scheduledPayments.isEmpty() ? 0.0 : 
                (double) exactMatches / scheduledPayments.size() * 100;
        
        return ReconciliationSummary.builder()
                .totalScheduledAmount(totalScheduled)
                .totalActualAmount(totalActual)
                .totalVarianceAmount(totalVariance)
                .scheduledPaymentCount(scheduledPayments.size())
                .actualPaymentCount(actualPayments.size())
                .exactMatchCount((int) exactMatches)
                .matchRate(matchRate)
                .build();
    }
    
    private BigDecimal calculateReconciliationAccuracy(List<PaymentVariance> variances) {
        if (variances.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        long exactMatches = variances.stream()
                .filter(v -> v.getVarianceType() == PaymentVarianceType.EXACT_MATCH)
                .count();
        
        return new BigDecimal(exactMatches)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(variances.size()), 2, java.math.RoundingMode.HALF_UP);
    }
    
    private Payment findMatchingPayment(ScheduledPayment scheduledPayment, List<Payment> actualPayments) {
        // Try to find exact match first
        Payment exactMatch = actualPayments.stream()
                .filter(p -> p.getLoanId().equals(scheduledPayment.getLoanId()))
                .filter(p -> p.getPaymentAmount().equals(scheduledPayment.getPaymentAmount()))
                .filter(p -> p.getPaymentDate().toLocalDate().equals(scheduledPayment.getDueDate()))
                .findFirst()
                .orElse(null);
        
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Try to find close match (within 3 days and 10% amount variance)
        return actualPayments.stream()
                .filter(p -> p.getLoanId().equals(scheduledPayment.getLoanId()))
                .filter(p -> isCloseAmountMatch(p.getPaymentAmount(), scheduledPayment.getPaymentAmount()))
                .filter(p -> isCloseDateMatch(p.getPaymentDate().toLocalDate(), scheduledPayment.getDueDate()))
                .findFirst()
                .orElse(null);
    }
    
    private ScheduledPayment findMatchingScheduledPayment(Payment actualPayment, List<ScheduledPayment> scheduledPayments) {
        return scheduledPayments.stream()
                .filter(s -> s.getLoanId().equals(actualPayment.getLoanId()))
                .filter(s -> isCloseAmountMatch(actualPayment.getPaymentAmount(), s.getPaymentAmount()))
                .filter(s -> isCloseDateMatch(actualPayment.getPaymentDate().toLocalDate(), s.getDueDate()))
                .findFirst()
                .orElse(null);
    }
    
    private boolean isCloseAmountMatch(Money amount1, Money amount2) {
        BigDecimal variance = amount1.getAmount().subtract(amount2.getAmount()).abs();
        BigDecimal tolerance = amount2.getAmount().multiply(new BigDecimal("0.1")); // 10% tolerance
        return variance.compareTo(tolerance) <= 0;
    }
    
    private boolean isCloseDateMatch(LocalDate date1, LocalDate date2) {
        long daysDifference = Math.abs(date1.until(date2, java.time.temporal.ChronoUnit.DAYS));
        return daysDifference <= 3; // 3-day tolerance
    }
    
    private PaymentDiscrepancy createMissingPaymentDiscrepancy(ScheduledPayment scheduledPayment) {
        return PaymentDiscrepancy.builder()
                .discrepancyId(DiscrepancyId.generate())
                .discrepancyType(PaymentDiscrepancyType.MISSING_PAYMENT)
                .scheduledPaymentId(scheduledPayment.getPaymentId())
                .expectedAmount(scheduledPayment.getPaymentAmount())
                .expectedDate(scheduledPayment.getDueDate())
                .description("Scheduled payment is missing")
                .severity(DiscrepancySeverity.HIGH)
                .detectedDate(LocalDateTime.now())
                .build();
    }
    
    private PaymentDiscrepancy createAmountDiscrepancy(ScheduledPayment scheduled, Payment actual) {
        Money variance = actual.getPaymentAmount().subtract(scheduled.getPaymentAmount());
        
        return PaymentDiscrepancy.builder()
                .discrepancyId(DiscrepancyId.generate())
                .discrepancyType(PaymentDiscrepancyType.AMOUNT_MISMATCH)
                .scheduledPaymentId(scheduled.getPaymentId())
                .actualPaymentId(actual.getPaymentId())
                .expectedAmount(scheduled.getPaymentAmount())
                .actualAmount(actual.getPaymentAmount())
                .amountVariance(variance)
                .description(String.format("Amount variance: %s", variance))
                .severity(determineAmountDiscrepancySeverity(variance))
                .detectedDate(LocalDateTime.now())
                .build();
    }
    
    private PaymentDiscrepancy createDateDiscrepancy(ScheduledPayment scheduled, Payment actual) {
        int daysDifference = (int) scheduled.getDueDate().until(actual.getPaymentDate().toLocalDate(), 
                java.time.temporal.ChronoUnit.DAYS);
        
        return PaymentDiscrepancy.builder()
                .discrepancyId(DiscrepancyId.generate())
                .discrepancyType(PaymentDiscrepancyType.DATE_MISMATCH)
                .scheduledPaymentId(scheduled.getPaymentId())
                .actualPaymentId(actual.getPaymentId())
                .expectedDate(scheduled.getDueDate())
                .actualDate(actual.getPaymentDate().toLocalDate())
                .dayVariance(daysDifference)
                .description(String.format("Date variance: %d days", daysDifference))
                .severity(determineDateDiscrepancySeverity(daysDifference))
                .detectedDate(LocalDateTime.now())
                .build();
    }
    
    private PaymentDiscrepancy createUnexpectedPaymentDiscrepancy(Payment actual) {
        return PaymentDiscrepancy.builder()
                .discrepancyId(DiscrepancyId.generate())
                .discrepancyType(PaymentDiscrepancyType.UNEXPECTED_PAYMENT)
                .actualPaymentId(actual.getPaymentId())
                .actualAmount(actual.getPaymentAmount())
                .actualDate(actual.getPaymentDate().toLocalDate())
                .description("Payment not found in schedule")
                .severity(DiscrepancySeverity.MEDIUM)
                .detectedDate(LocalDateTime.now())
                .build();
    }
    
    private boolean canAutoResolve(PaymentDiscrepancy discrepancy, PaymentReconciliationConfig config) {
        if (!config.isEnableAutoResolution()) {
            return false;
        }
        
        return switch (discrepancy.getDiscrepancyType()) {
            case AMOUNT_MISMATCH -> discrepancy.getAmountVariance() != null && 
                    discrepancy.getAmountVariance().getAmount().abs()
                            .compareTo(config.getAutoResolveThreshold().getAmount()) <= 0;
            case DATE_MISMATCH -> Math.abs(discrepancy.getDayVariance()) <= config.getAutoResolveDateVarianceDays();
            default -> false;
        };
    }
    
    private PaymentDiscrepancy autoResolveDiscrepancy(PaymentDiscrepancy discrepancy, PaymentReconciliationConfig config) {
        return discrepancy.toBuilder()
                .status(DiscrepancyStatus.AUTO_RESOLVED)
                .resolutionDate(LocalDateTime.now())
                .resolutionMethod("AUTO_RESOLVED_WITHIN_TOLERANCE")
                .resolutionNotes("Automatically resolved within configured tolerance limits")
                .build();
    }
    
    private double calculateAutoResolutionRate(int autoResolved, int total) {
        return total > 0 ? (double) autoResolved / total * 100 : 0.0;
    }
    
    private PaymentVarianceType determineVarianceType(Money amountVariance, int dayVariance) {
        if (amountVariance.isZero() && dayVariance == 0) {
            return PaymentVarianceType.EXACT_MATCH;
        } else if (amountVariance.isZero()) {
            return PaymentVarianceType.DATE_VARIANCE_ONLY;
        } else if (dayVariance == 0) {
            return PaymentVarianceType.AMOUNT_VARIANCE_ONLY;
        } else {
            return PaymentVarianceType.BOTH_VARIANCE;
        }
    }
    
    private PaymentVarianceStatistics calculateVarianceStatistics(List<PaymentVariance> variances) {
        if (variances.isEmpty()) {
            return PaymentVarianceStatistics.builder()
                    .averageVariance(Money.zero("USD"))
                    .maxVariance(Money.zero("USD"))
                    .minVariance(Money.zero("USD"))
                    .build();
        }
        
        Money totalVariance = variances.stream()
                .map(PaymentVariance::getAmountVariance)
                .reduce(Money.zero("USD"), Money::add);
        
        Money averageVariance = totalVariance.divide(new BigDecimal(variances.size()));
        
        Money maxVariance = variances.stream()
                .map(PaymentVariance::getAmountVariance)
                .max((v1, v2) -> v1.getAmount().compareTo(v2.getAmount()))
                .orElse(Money.zero("USD"));
        
        Money minVariance = variances.stream()
                .map(PaymentVariance::getAmountVariance)
                .min((v1, v2) -> v1.getAmount().compareTo(v2.getAmount()))
                .orElse(Money.zero("USD"));
        
        return PaymentVarianceStatistics.builder()
                .averageVariance(averageVariance)
                .maxVariance(maxVariance)
                .minVariance(minVariance)
                .varianceCount(variances.size())
                .build();
    }
    
    private Map<LocalDate, PaymentVariance> groupVariancesByMonth(List<PaymentVariance> variances) {
        return variances.stream()
                .collect(Collectors.toMap(
                        variance -> variance.getScheduledDate().withDayOfMonth(1), // Group by month
                        variance -> variance,
                        (existing, replacement) -> existing // Keep first occurrence
                ));
    }
    
    private VarianceTrend calculateVarianceTrend(List<PaymentVariance> variances) {
        // Simplified trend calculation
        if (variances.size() < 2) {
            return VarianceTrend.STABLE;
        }
        
        // Compare first half vs second half
        int midpoint = variances.size() / 2;
        double firstHalfAverage = variances.subList(0, midpoint).stream()
                .mapToDouble(v -> v.getAmountVariance().getAmount().doubleValue())
                .average()
                .orElse(0.0);
        
        double secondHalfAverage = variances.subList(midpoint, variances.size()).stream()
                .mapToDouble(v -> v.getAmountVariance().getAmount().doubleValue())
                .average()
                .orElse(0.0);
        
        if (secondHalfAverage > firstHalfAverage * 1.1) {
            return VarianceTrend.INCREASING;
        } else if (secondHalfAverage < firstHalfAverage * 0.9) {
            return VarianceTrend.DECREASING;
        } else {
            return VarianceTrend.STABLE;
        }
    }
    
    private DiscrepancySeverity determineAmountDiscrepancySeverity(Money variance) {
        BigDecimal absVariance = variance.getAmount().abs();
        
        if (absVariance.compareTo(new BigDecimal("1000")) >= 0) {
            return DiscrepancySeverity.HIGH;
        } else if (absVariance.compareTo(new BigDecimal("100")) >= 0) {
            return DiscrepancySeverity.MEDIUM;
        } else {
            return DiscrepancySeverity.LOW;
        }
    }
    
    private DiscrepancySeverity determineDateDiscrepancySeverity(int daysDifference) {
        int absDays = Math.abs(daysDifference);
        
        if (absDays >= 30) {
            return DiscrepancySeverity.HIGH;
        } else if (absDays >= 7) {
            return DiscrepancySeverity.MEDIUM;
        } else {
            return DiscrepancySeverity.LOW;
        }
    }
}