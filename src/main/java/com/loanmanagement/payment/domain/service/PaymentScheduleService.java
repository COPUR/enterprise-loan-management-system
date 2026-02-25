package com.loanmanagement.payment.domain.service;

import com.loanmanagement.loan.domain.model.LoanId;
import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Domain Service for Payment Schedule Management
 * Handles payment schedule generation, modification, and management
 */
@Slf4j
@Service
public class PaymentScheduleService {

    /**
     * Generate a new payment schedule
     */
    public PaymentSchedule generatePaymentSchedule(PaymentScheduleRequest scheduleRequest) {
        log.info("Generating payment schedule for loan: {}, frequency: {}", 
                scheduleRequest.getLoanId(), scheduleRequest.getFrequency());
        
        validateScheduleRequest(scheduleRequest);
        
        List<ScheduledPayment> scheduledPayments = generateScheduledPayments(scheduleRequest);
        
        return PaymentSchedule.builder()
                .scheduleId(PaymentScheduleId.generate())
                .loanId(scheduleRequest.getLoanId())
                .customerId(scheduleRequest.getCustomerId())
                .frequency(scheduleRequest.getFrequency())
                .paymentAmount(scheduleRequest.getPaymentAmount())
                .startDate(scheduleRequest.getStartDate())
                .endDate(scheduleRequest.getEndDate())
                .status(PaymentScheduleStatus.ACTIVE)
                .scheduledPayments(scheduledPayments)
                .gracePeriodDays(scheduleRequest.getGracePeriodDays())
                .holidayAdjustmentRule(scheduleRequest.getHolidayAdjustmentRule())
                .createdDate(LocalDate.now())
                .lastModifiedDate(LocalDate.now())
                .totalPayments(scheduledPayments.size())
                .totalScheduledAmount(calculateTotalScheduledAmount(scheduledPayments))
                .build();
    }
    
    /**
     * Modify an existing payment schedule
     */
    public PaymentSchedule modifyPaymentSchedule(PaymentSchedule originalSchedule, 
                                               PaymentScheduleModification modification) {
        log.info("Modifying payment schedule: {}, type: {}", 
                originalSchedule.getScheduleId(), modification.getModificationType());
        
        validateModification(originalSchedule, modification);
        
        PaymentSchedule modifiedSchedule = applyModification(originalSchedule, modification);
        
        List<PaymentScheduleModification> modifications = new ArrayList<>(originalSchedule.getModifications());
        modifications.add(modification);
        
        return modifiedSchedule.toBuilder()
                .modifications(modifications)
                .lastModifiedDate(LocalDate.now())
                .version(originalSchedule.getVersion() + 1)
                .build();
    }
    
    /**
     * Suspend a payment schedule
     */
    public PaymentSchedule suspendPaymentSchedule(PaymentSchedule activeSchedule, 
                                                 PaymentScheduleSuspension suspension) {
        log.info("Suspending payment schedule: {} from {} to {}", 
                activeSchedule.getScheduleId(), 
                suspension.getSuspensionStartDate(), 
                suspension.getSuspensionEndDate());
        
        validateSuspension(activeSchedule, suspension);
        
        List<ScheduledPayment> updatedPayments = markPaymentsAsSuspended(
                activeSchedule.getScheduledPayments(), suspension);
        
        List<PaymentScheduleSuspension> suspensions = new ArrayList<>(activeSchedule.getSuspensions());
        suspensions.add(suspension);
        
        return activeSchedule.toBuilder()
                .status(PaymentScheduleStatus.SUSPENDED)
                .scheduledPayments(updatedPayments)
                .suspensions(suspensions)
                .lastModifiedDate(LocalDate.now())
                .build();
    }
    
    /**
     * Resume a suspended payment schedule
     */
    public PaymentSchedule resumePaymentSchedule(PaymentSchedule suspendedSchedule, 
                                               LocalDate resumeDate) {
        log.info("Resuming payment schedule: {} on {}", suspendedSchedule.getScheduleId(), resumeDate);
        
        if (suspendedSchedule.getStatus() != PaymentScheduleStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended schedules can be resumed");
        }
        
        List<ScheduledPayment> updatedPayments = markPaymentsAsActive(
                suspendedSchedule.getScheduledPayments(), resumeDate);
        
        return suspendedSchedule.toBuilder()
                .status(PaymentScheduleStatus.ACTIVE)
                .scheduledPayments(updatedPayments)
                .lastModifiedDate(LocalDate.now())
                .build();
    }
    
    /**
     * Get upcoming payments from a schedule
     */
    public List<ScheduledPayment> getUpcomingPayments(PaymentSchedule schedule, int daysAhead) {
        LocalDate cutoffDate = LocalDate.now().plusDays(daysAhead);
        
        return schedule.getScheduledPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SCHEDULED)
                .filter(payment -> payment.getDueDate().isAfter(LocalDate.now()) && 
                                 payment.getDueDate().isBefore(cutoffDate))
                .toList();
    }
    
    /**
     * Get overdue payments from a schedule
     */
    public List<ScheduledPayment> getOverduePayments(PaymentSchedule schedule) {
        LocalDate today = LocalDate.now();
        
        return schedule.getScheduledPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SCHEDULED)
                .filter(payment -> payment.getDueDate().isBefore(today))
                .toList();
    }
    
    /**
     * Calculate schedule health metrics
     */
    public PaymentScheduleHealthMetrics calculateScheduleHealth(PaymentSchedule schedule) {
        List<ScheduledPayment> allPayments = schedule.getScheduledPayments();
        List<ScheduledPayment> overduePayments = getOverduePayments(schedule);
        List<ScheduledPayment> completedPayments = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                .toList();
        
        double onTimePaymentRate = allPayments.isEmpty() ? 0.0 : 
                (double) completedPayments.size() / allPayments.size() * 100;
        
        double overdueRate = allPayments.isEmpty() ? 0.0 : 
                (double) overduePayments.size() / allPayments.size() * 100;
        
        PaymentScheduleHealthStatus healthStatus = determineHealthStatus(onTimePaymentRate, overdueRate);
        
        return PaymentScheduleHealthMetrics.builder()
                .scheduleId(schedule.getScheduleId())
                .totalPayments(allPayments.size())
                .completedPayments(completedPayments.size())
                .overduePayments(overduePayments.size())
                .onTimePaymentRate(onTimePaymentRate)
                .overdueRate(overdueRate)
                .healthStatus(healthStatus)
                .lastCalculated(LocalDate.now())
                .build();
    }
    
    // Private helper methods
    
    private void validateScheduleRequest(PaymentScheduleRequest request) {
        if (request.getLoanId() == null) {
            throw new IllegalArgumentException("Loan ID is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (request.getPaymentAmount() == null || request.getPaymentAmount().getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (request.getFrequency() == null) {
            throw new IllegalArgumentException("Payment frequency is required");
        }
    }
    
    private List<ScheduledPayment> generateScheduledPayments(PaymentScheduleRequest request) {
        List<ScheduledPayment> payments = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        int paymentNumber = 1;
        
        while (currentDate.isBefore(request.getEndDate()) || currentDate.isEqual(request.getEndDate())) {
            LocalDate dueDate = calculateNextPaymentDate(currentDate, request.getFrequency(), request);
            
            if (dueDate.isAfter(request.getEndDate())) {
                break;
            }
            
            // Apply holiday adjustments if configured
            LocalDate adjustedDueDate = applyHolidayAdjustments(dueDate, request);
            
            ScheduledPayment payment = ScheduledPayment.builder()
                    .paymentId(PaymentId.generate())
                    .loanId(request.getLoanId())
                    .customerId(request.getCustomerId())
                    .paymentNumber(paymentNumber)
                    .dueDate(adjustedDueDate)
                    .originalDueDate(dueDate)
                    .paymentAmount(request.getPaymentAmount())
                    .status(PaymentStatus.SCHEDULED)
                    .gracePeriodDays(request.getGracePeriodDays())
                    .lateFeeDate(calculateLateFeeDate(adjustedDueDate, request.getGracePeriodDays()))
                    .holidayAdjusted(!dueDate.equals(adjustedDueDate))
                    .build();
            
            payments.add(payment);
            currentDate = getNextPaymentBaseDate(currentDate, request.getFrequency());
            paymentNumber++;
        }
        
        return payments;
    }
    
    private LocalDate calculateNextPaymentDate(LocalDate baseDate, PaymentFrequency frequency, 
                                             PaymentScheduleRequest request) {
        return switch (frequency) {
            case MONTHLY -> {
                if (request.getDayOfMonth() != null) {
                    yield baseDate.withDayOfMonth(Math.min(request.getDayOfMonth(), 
                            baseDate.lengthOfMonth()));
                } else {
                    yield baseDate;
                }
            }
            case BI_WEEKLY -> {
                if (request.getDayOfWeek() != null) {
                    yield baseDate.with(DayOfWeek.of(request.getDayOfWeek()));
                } else {
                    yield baseDate;
                }
            }
            case WEEKLY -> {
                if (request.getDayOfWeek() != null) {
                    yield baseDate.with(DayOfWeek.of(request.getDayOfWeek()));
                } else {
                    yield baseDate;
                }
            }
            case QUARTERLY -> baseDate.plusMonths(3);
            case SEMI_ANNUALLY -> baseDate.plusMonths(6);
            case ANNUALLY -> baseDate.plusYears(1);
        };
    }
    
    private LocalDate getNextPaymentBaseDate(LocalDate currentDate, PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> currentDate.plusMonths(1);
            case BI_WEEKLY -> currentDate.plusWeeks(2);
            case WEEKLY -> currentDate.plusWeeks(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentDate.plusMonths(6);
            case ANNUALLY -> currentDate.plusYears(1);
        };
    }
    
    private LocalDate applyHolidayAdjustments(LocalDate dueDate, PaymentScheduleRequest request) {
        if (request.getHolidayAdjustmentRule() == null || 
            request.getHolidayAdjustmentRule() == HolidayAdjustmentRule.NO_ADJUSTMENT) {
            return dueDate;
        }
        
        // Check if date falls on weekend
        DayOfWeek dayOfWeek = dueDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return switch (request.getHolidayAdjustmentRule()) {
                case NEXT_BUSINESS_DAY -> adjustToNextBusinessDay(dueDate);
                case PREVIOUS_BUSINESS_DAY -> adjustToPreviousBusinessDay(dueDate);
                default -> dueDate;
            };
        }
        
        // Check if date is a holiday (simplified - would use actual holiday calendar)
        if (request.getHolidays() != null && request.getHolidays().contains(dueDate)) {
            return switch (request.getHolidayAdjustmentRule()) {
                case NEXT_BUSINESS_DAY -> adjustToNextBusinessDay(dueDate);
                case PREVIOUS_BUSINESS_DAY -> adjustToPreviousBusinessDay(dueDate);
                default -> dueDate;
            };
        }
        
        return dueDate;
    }
    
    private LocalDate adjustToNextBusinessDay(LocalDate date) {
        LocalDate adjusted = date;
        while (adjusted.getDayOfWeek().getValue() > 5) { // Weekend
            adjusted = adjusted.plusDays(1);
        }
        return adjusted;
    }
    
    private LocalDate adjustToPreviousBusinessDay(LocalDate date) {
        LocalDate adjusted = date;
        while (adjusted.getDayOfWeek().getValue() > 5) { // Weekend
            adjusted = adjusted.minusDays(1);
        }
        return adjusted;
    }
    
    private LocalDate calculateLateFeeDate(LocalDate dueDate, Integer gracePeriodDays) {
        if (gracePeriodDays == null || gracePeriodDays <= 0) {
            return dueDate;
        }
        return dueDate.plusDays(gracePeriodDays);
    }
    
    private Money calculateTotalScheduledAmount(List<ScheduledPayment> payments) {
        return payments.stream()
                .map(ScheduledPayment::getPaymentAmount)
                .reduce(Money.zero("USD"), Money::add);
    }
    
    private void validateModification(PaymentSchedule schedule, PaymentScheduleModification modification) {
        if (schedule.getStatus() == PaymentScheduleStatus.CANCELLED) {
            throw new IllegalStateException("Cannot modify a cancelled schedule");
        }
        
        if (modification.getEffectiveDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Modification effective date cannot be in the past");
        }
    }
    
    private PaymentSchedule applyModification(PaymentSchedule originalSchedule, 
                                            PaymentScheduleModification modification) {
        return switch (modification.getModificationType()) {
            case AMOUNT_CHANGE -> applyAmountChange(originalSchedule, modification);
            case FREQUENCY_CHANGE -> applyFrequencyChange(originalSchedule, modification);
            case DATE_CHANGE -> applyDateChange(originalSchedule, modification);
            case SUSPENSION -> applySuspension(originalSchedule, modification);
            case CANCELLATION -> applyCancellation(originalSchedule, modification);
        };
    }
    
    private PaymentSchedule applyAmountChange(PaymentSchedule schedule, PaymentScheduleModification modification) {
        List<ScheduledPayment> updatedPayments = schedule.getScheduledPayments().stream()
                .map(payment -> {
                    if (payment.getDueDate().isAfter(modification.getEffectiveDate()) || 
                        payment.getDueDate().isEqual(modification.getEffectiveDate())) {
                        return payment.toBuilder()
                                .paymentAmount(modification.getNewPaymentAmount())
                                .build();
                    }
                    return payment;
                })
                .toList();
        
        return schedule.toBuilder()
                .paymentAmount(modification.getNewPaymentAmount())
                .scheduledPayments(updatedPayments)
                .build();
    }
    
    private PaymentSchedule applyFrequencyChange(PaymentSchedule schedule, PaymentScheduleModification modification) {
        // For frequency changes, regenerate the schedule from the effective date
        LocalDate effectiveDate = modification.getEffectiveDate();
        
        List<ScheduledPayment> keepPayments = schedule.getScheduledPayments().stream()
                .filter(payment -> payment.getDueDate().isBefore(effectiveDate))
                .toList();
        
        // Generate new payments from effective date with new frequency
        PaymentScheduleRequest newRequest = PaymentScheduleRequest.builder()
                .loanId(schedule.getLoanId())
                .customerId(schedule.getCustomerId())
                .startDate(effectiveDate)
                .endDate(schedule.getEndDate())
                .paymentAmount(modification.getNewPaymentAmount())
                .frequency(modification.getNewFrequency())
                .build();
        
        List<ScheduledPayment> newPayments = generateScheduledPayments(newRequest);
        
        List<ScheduledPayment> allPayments = new ArrayList<>(keepPayments);
        allPayments.addAll(newPayments);
        
        return schedule.toBuilder()
                .frequency(modification.getNewFrequency())
                .paymentAmount(modification.getNewPaymentAmount())
                .scheduledPayments(allPayments)
                .build();
    }
    
    private PaymentSchedule applyDateChange(PaymentSchedule schedule, PaymentScheduleModification modification) {
        // Implementation for date changes
        return schedule; // Simplified for now
    }
    
    private PaymentSchedule applySuspension(PaymentSchedule schedule, PaymentScheduleModification modification) {
        // Implementation for suspension
        return schedule; // Simplified for now
    }
    
    private PaymentSchedule applyCancellation(PaymentSchedule schedule, PaymentScheduleModification modification) {
        return schedule.toBuilder()
                .status(PaymentScheduleStatus.CANCELLED)
                .build();
    }
    
    private void validateSuspension(PaymentSchedule schedule, PaymentScheduleSuspension suspension) {
        if (schedule.getStatus() != PaymentScheduleStatus.ACTIVE) {
            throw new IllegalStateException("Only active schedules can be suspended");
        }
        
        if (suspension.getSuspensionStartDate().isAfter(suspension.getSuspensionEndDate())) {
            throw new IllegalArgumentException("Suspension start date must be before end date");
        }
    }
    
    private List<ScheduledPayment> markPaymentsAsSuspended(List<ScheduledPayment> payments, 
                                                          PaymentScheduleSuspension suspension) {
        return payments.stream()
                .map(payment -> {
                    if (payment.getDueDate().isAfter(suspension.getSuspensionStartDate()) && 
                        payment.getDueDate().isBefore(suspension.getSuspensionEndDate())) {
                        return payment.toBuilder()
                                .status(PaymentStatus.SUSPENDED)
                                .build();
                    }
                    return payment;
                })
                .toList();
    }
    
    private List<ScheduledPayment> markPaymentsAsActive(List<ScheduledPayment> payments, LocalDate resumeDate) {
        return payments.stream()
                .map(payment -> {
                    if (payment.getStatus() == PaymentStatus.SUSPENDED && 
                        payment.getDueDate().isAfter(resumeDate)) {
                        return payment.toBuilder()
                                .status(PaymentStatus.SCHEDULED)
                                .build();
                    }
                    return payment;
                })
                .toList();
    }
    
    private PaymentScheduleHealthStatus determineHealthStatus(double onTimeRate, double overdueRate) {
        if (overdueRate > 20.0) {
            return PaymentScheduleHealthStatus.CRITICAL;
        } else if (overdueRate > 10.0 || onTimeRate < 80.0) {
            return PaymentScheduleHealthStatus.WARNING;
        } else {
            return PaymentScheduleHealthStatus.HEALTHY;
        }
    }
}