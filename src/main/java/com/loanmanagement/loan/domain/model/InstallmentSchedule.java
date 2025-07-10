package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Installment schedule containing all scheduled payments for a loan.
 */
@Data
@Builder
@With
public class InstallmentSchedule {
    
    @NotNull
    private final LoanId loanId;
    
    @NotNull
    private final String scheduleId;
    
    @NotNull
    private final LocalDate scheduleDate;
    
    @NotNull
    private final List<ScheduledInstallment> scheduledInstallments;
    
    @NotNull
    private final BigDecimal totalScheduledAmount;
    
    @NotNull
    private final BigDecimal totalPrincipalAmount;
    
    @NotNull
    private final BigDecimal totalInterestAmount;
    
    private final BigDecimal totalFeesAmount;
    
    private final BigDecimal totalEscrowAmount;
    
    @NotNull
    private final PaymentFrequency paymentFrequency;
    
    @NotNull
    private final LocalDate firstPaymentDate;
    
    @NotNull
    private final LocalDate lastPaymentDate;
    
    private final Integer totalPayments;
    
    private final boolean includesEscrow;
    
    private final boolean includesFees;
    
    private final String scheduleType;
    
    private final LocalDate lastModifiedDate;
    
    private final String modifiedBy;
    
    /**
     * Gets installment by payment number.
     */
    public ScheduledInstallment getInstallmentByNumber(Integer paymentNumber) {
        return scheduledInstallments.stream()
                .filter(inst -> inst.getInstallmentNumber().equals(paymentNumber))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets installments for a specific date range.
     */
    public List<ScheduledInstallment> getInstallmentsForDateRange(LocalDate startDate, LocalDate endDate) {
        return scheduledInstallments.stream()
                .filter(inst -> !inst.getDueDate().isBefore(startDate) && 
                              !inst.getDueDate().isAfter(endDate))
                .toList();
    }
    
    /**
     * Gets overdue installments.
     */
    public List<ScheduledInstallment> getOverdueInstallments() {
        LocalDate currentDate = LocalDate.now();
        return scheduledInstallments.stream()
                .filter(inst -> inst.getDueDate().isBefore(currentDate) && 
                              !inst.isPaid())
                .toList();
    }
    
    /**
     * Gets upcoming installments within specified days.
     */
    public List<ScheduledInstallment> getUpcomingInstallments(Integer daysAhead) {
        LocalDate currentDate = LocalDate.now();
        LocalDate futureDate = currentDate.plusDays(daysAhead);
        
        return scheduledInstallments.stream()
                .filter(inst -> !inst.getDueDate().isBefore(currentDate) && 
                              !inst.getDueDate().isAfter(futureDate) &&
                              !inst.isPaid())
                .toList();
    }
    
    /**
     * Calculates remaining balance at a specific payment number.
     */
    public BigDecimal getRemainingBalanceAtPayment(Integer paymentNumber) {
        return scheduledInstallments.stream()
                .filter(inst -> inst.getInstallmentNumber() > paymentNumber)
                .map(ScheduledInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates total payments made to date.
     */
    public BigDecimal getTotalPaymentsMade() {
        return scheduledInstallments.stream()
                .filter(ScheduledInstallment::isPaid)
                .map(ScheduledInstallment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates total principal paid to date.
     */
    public BigDecimal getTotalPrincipalPaid() {
        return scheduledInstallments.stream()
                .filter(ScheduledInstallment::isPaid)
                .map(ScheduledInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates total interest paid to date.
     */
    public BigDecimal getTotalInterestPaid() {
        return scheduledInstallments.stream()
                .filter(ScheduledInstallment::isPaid)
                .map(ScheduledInstallment::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Gets the current outstanding balance.
     */
    public BigDecimal getCurrentOutstandingBalance() {
        return scheduledInstallments.stream()
                .filter(inst -> !inst.isPaid())
                .map(ScheduledInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates the next payment due.
     */
    public ScheduledInstallment getNextPaymentDue() {
        LocalDate currentDate = LocalDate.now();
        return scheduledInstallments.stream()
                .filter(inst -> !inst.isPaid() && 
                              !inst.getDueDate().isBefore(currentDate))
                .min((inst1, inst2) -> inst1.getDueDate().compareTo(inst2.getDueDate()))
                .orElse(null);
    }
    
    /**
     * Calculates payment progress percentage.
     */
    public BigDecimal calculatePaymentProgress() {
        if (totalScheduledAmount == null || totalScheduledAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal paymentsMade = getTotalPaymentsMade();
        return paymentsMade.divide(totalScheduledAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Validates the installment schedule.
     */
    public boolean isValid() {
        if (loanId == null || scheduleId == null || scheduledInstallments == null) {
            return false;
        }
        
        if (scheduledInstallments.isEmpty()) {
            return false;
        }
        
        if (totalScheduledAmount == null || totalScheduledAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Validate that installment numbers are sequential
        for (int i = 0; i < scheduledInstallments.size(); i++) {
            if (!scheduledInstallments.get(i).getInstallmentNumber().equals(i + 1)) {
                return false;
            }
        }
        
        return true;
    }
}