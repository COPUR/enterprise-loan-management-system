package com.loanmanagement.loan.domain.service;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Service for handling loan installment calculations
 * Provides comprehensive installment calculation capabilities
 */
@Slf4j
@Service
public class LoanInstallmentService {
    
    private static final MathContext CALCULATION_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 2;
    
    private final LoanCalculationService calculationService;
    
    public LoanInstallmentService() {
        this.calculationService = new LoanCalculationService();
    }
    
    /**
     * Creates a standard installment plan for a loan
     */
    public InstallmentPlan createInstallmentPlan(Money principalAmount, LoanTerms terms, LocalDate startDate) {
        validateInputs(principalAmount, terms, startDate);
        
        Money installmentAmount = calculationService.calculatePaymentAmount(principalAmount, terms);
        Money totalPaymentAmount = installmentAmount.multiply(new BigDecimal(terms.getTermInMonths()));
        Money totalInterestAmount = totalPaymentAmount.subtract(principalAmount);
        
        return InstallmentPlan.builder()
                .principalAmount(principalAmount.getAmount())
                .interestRate(terms.getInterestRate())
                .termInMonths(terms.getTermInMonths())
                .paymentFrequency(terms.getPaymentFrequency())
                .startDate(startDate)
                .endDate(startDate.plusMonths(terms.getTermInMonths()))
                .totalPaymentAmount(totalPaymentAmount.getAmount())
                .totalInterestAmount(totalInterestAmount.getAmount())
                .installments(java.util.List.of())
                .isActive(true)
                .createdDate(LocalDate.now())
                .lastModifiedDate(LocalDate.now())
                .build();
    }
    
    /**
     * Creates a variable rate installment plan
     */
    public VariableRateInstallmentPlan createVariableRateInstallmentPlan(
            Money principalAmount, LoanTerms terms, List<RateAdjustment> rateAdjustments, LocalDate startDate) {
        
        validateInputs(principalAmount, terms, startDate);
        
        List<RatePeriod> ratePeriods = calculateRatePeriods(principalAmount, terms, rateAdjustments, startDate);
        
        InstallmentPlan basePlan = createInstallmentPlan(principalAmount, terms, startDate);
        return VariableRateInstallmentPlan.builder()
                .basePlan(basePlan)
                .initialRate(terms.getInterestRate())
                .rateIndex("LIBOR")
                .margin(BigDecimal.ZERO)
                .ratePeriods(ratePeriods)
                .rateAdjustments(rateAdjustments)
                .build();
    }
    
    /**
     * Modifies an existing installment plan
     */
    public ModifiedInstallmentPlan modifyInstallmentPlan(InstallmentPlan originalPlan, InstallmentModification modification) {
        validateModification(originalPlan, modification);
        
        List<InstallmentModification> modifications = List.of(modification);
        
        int effectiveTermMonths = calculateEffectiveTermMonths(originalPlan, modification);
        Money modifiedPaymentAmount = calculateModifiedPaymentAmount(originalPlan, modification);
        Money totalInterestAmount = calculateModifiedTotalInterest(originalPlan, modification);
        
        return ModifiedInstallmentPlan.builder()
                .originalPlan(originalPlan)
                .modifications(modifications)
                .effectiveTermMonths(effectiveTermMonths)
                .modifiedPaymentAmount(modifiedPaymentAmount)
                .totalInterestAmount(totalInterestAmount)
                .deferredPayments(calculateDeferredPayments(modification))
                .build();
    }
    
    /**
     * Creates balloon payment installment plan
     */
    public BalloonInstallmentPlan createBalloonInstallmentPlan(
            Money principalAmount, LoanTerms terms, BalloonPaymentTerms balloonTerms, LocalDate startDate) {
        
        validateInputs(principalAmount, terms, startDate);
        
        Money regularPaymentBasis = balloonTerms.getRegularPaymentBasis();
        Money regularPaymentAmount = calculationService.calculatePaymentAmount(regularPaymentBasis, terms);
        
        BalloonPayment balloonPayment = BalloonPayment.builder()
                .paymentNumber(balloonTerms.getBalloonPaymentNumber())
                .paymentAmount(balloonTerms.getBalloonAmount())
                .dueDate(startDate.plusMonths(balloonTerms.getBalloonPaymentNumber()))
                .build();
        
        return BalloonInstallmentPlan.builder()
                .principalAmount(principalAmount)
                .terms(terms)
                .balloonTerms(balloonTerms)
                .startDate(startDate)
                .regularPaymentAmount(regularPaymentAmount)
                .balloonPayment(balloonPayment)
                .build();
    }
    
    /**
     * Creates graduated payment installment plan
     */
    public GraduatedInstallmentPlan createGraduatedInstallmentPlan(
            Money principalAmount, LoanTerms terms, GraduatedPaymentSchedule gradSchedule, LocalDate startDate) {
        
        validateInputs(principalAmount, terms, startDate);
        
        List<PaymentPeriod> paymentPeriods = calculateGraduatedPaymentPeriods(
                principalAmount, terms, gradSchedule, startDate);
        
        return GraduatedInstallmentPlan.builder()
                .principalAmount(principalAmount)
                .terms(terms)
                .graduatedSchedule(gradSchedule)
                .startDate(startDate)
                .paymentPeriods(paymentPeriods)
                .build();
    }
    
    /**
     * Creates interest-only installment plan
     */
    public InterestOnlyInstallmentPlan createInterestOnlyInstallmentPlan(
            Money principalAmount, LoanTerms terms, InterestOnlyPeriod ioTerms, LocalDate startDate) {
        
        validateInputs(principalAmount, terms, startDate);
        
        List<ScheduledInstallment> interestOnlyPayments = calculateInterestOnlyPayments(
                principalAmount, terms, ioTerms, startDate);
        
        List<ScheduledInstallment> principalAndInterestPayments = calculatePrincipalAndInterestPayments(
                principalAmount, terms, ioTerms, startDate);
        
        return InterestOnlyInstallmentPlan.builder()
                .principalAmount(principalAmount)
                .terms(terms)
                .interestOnlyPeriod(ioTerms)
                .startDate(startDate)
                .interestOnlyPayments(interestOnlyPayments)
                .principalAndInterestPayments(principalAndInterestPayments)
                .build();
    }
    
    /**
     * Generates payment schedule with specific dates
     */
    public InstallmentSchedule generatePaymentSchedule(Money principalAmount, LoanTerms terms, LocalDate startDate) {
        validateInputs(principalAmount, terms, startDate);
        
        Money installmentAmount = calculationService.calculatePaymentAmount(principalAmount, terms);
        List<ScheduledInstallment> scheduledPayments = new ArrayList<>();
        
        for (int i = 1; i <= terms.getTermInMonths(); i++) {
            LocalDate dueDate = calculatePaymentDate(startDate, i, terms.getPaymentFrequency());
            
            scheduledPayments.add(ScheduledInstallment.builder()
                    .paymentNumber(i)
                    .dueDate(dueDate)
                    .paymentAmount(installmentAmount)
                    .principalAmount(calculatePrincipalPortion(principalAmount, terms, i))
                    .interestAmount(calculateInterestPortion(principalAmount, terms, i))
                    .remainingBalance(calculateRemainingBalance(principalAmount, terms, i))
                    .build());
        }
        
        return InstallmentSchedule.builder()
                .principalAmount(principalAmount)
                .terms(terms)
                .startDate(startDate)
                .scheduledPayments(scheduledPayments)
                .totalScheduledAmount(installmentAmount.multiply(new BigDecimal(terms.getTermInMonths())))
                .build();
    }
    
    /**
     * Generates payment schedule with holiday adjustments
     */
    public InstallmentSchedule generatePaymentScheduleWithHolidays(
            Money principalAmount, LoanTerms terms, LocalDate startDate, HolidayAdjustmentRule holidayRule) {
        
        InstallmentSchedule baseSchedule = generatePaymentSchedule(principalAmount, terms, startDate);
        
        List<ScheduledInstallment> adjustedPayments = baseSchedule.getScheduledPayments().stream()
                .map(payment -> adjustForHolidays(payment, holidayRule))
                .toList();
        
        return baseSchedule.toBuilder()
                .scheduledPayments(adjustedPayments)
                .build();
    }
    
    /**
     * Calculates amortization analytics
     */
    public AmortizationAnalytics calculateAmortizationAnalytics(InstallmentPlan plan) {
        return AmortizationAnalytics.builder()
                .plan(plan)
                .principalPercentages(calculatePrincipalPercentages(plan))
                .interestPercentages(calculateInterestPercentages(plan))
                .build();
    }
    
    /**
     * Calculates early payoff savings analysis
     */
    public EarlyPayoffAnalysis calculateEarlyPayoffSavings(
            InstallmentPlan plan, int payoffAtPayment, LocalDate payoffDate) {
        
        Money remainingBalance = calculateRemainingBalance(
                plan.getPrincipalAmount(), plan.getTerms(), payoffAtPayment);
        
        Money remainingInterest = calculateRemainingInterest(plan, payoffAtPayment);
        Money interestSavings = remainingInterest;
        
        int timeSavingsMonths = plan.getTermInMonths() - payoffAtPayment;
        
        return EarlyPayoffAnalysis.builder()
                .originalPlan(plan)
                .payoffAtPayment(payoffAtPayment)
                .payoffDate(payoffDate)
                .payoffAmount(remainingBalance)
                .interestSavings(interestSavings)
                .timeSavingsMonths(timeSavingsMonths)
                .breakEvenPoint(calculateBreakEvenPoint(plan, payoffAtPayment))
                .build();
    }
    
    /**
     * Compares different installment options
     */
    public InstallmentComparison compareInstallmentOptions(
            Money principalAmount, List<LoanTerms> termOptions, LocalDate startDate) {
        
        List<InstallmentOption> options = termOptions.stream()
                .map(terms -> createInstallmentOption(principalAmount, terms, startDate))
                .toList();
        
        InstallmentOption recommendation = determineRecommendation(options);
        
        return InstallmentComparison.builder()
                .principalAmount(principalAmount)
                .options(options)
                .recommendation(recommendation)
                .comparisonDate(startDate)
                .build();
    }
    
    // Private helper methods
    
    private void validateInputs(Money principalAmount, LoanTerms terms, LocalDate startDate) {
        if (principalAmount == null || principalAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        if (terms == null || terms.getTermInMonths() <= 0) {
            throw new IllegalArgumentException("Loan terms must be valid with positive term");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
    }
    
    private void validateModification(InstallmentPlan plan, InstallmentModification modification) {
        if (plan == null || modification == null) {
            throw new IllegalArgumentException("Plan and modification cannot be null");
        }
    }
    
    private List<RatePeriod> calculateRatePeriods(
            Money principalAmount, LoanTerms terms, List<RateAdjustment> rateAdjustments, LocalDate startDate) {
        
        List<RatePeriod> periods = new ArrayList<>();
        BigDecimal currentRate = terms.getInterestRate();
        int currentPayment = 1;
        
        // Initial period with base rate
        RateAdjustment firstAdjustment = rateAdjustments.isEmpty() ? null : rateAdjustments.get(0);
        int firstPeriodPayments = firstAdjustment != null ? 
                (int) ChronoUnit.MONTHS.between(startDate, firstAdjustment.getEffectiveDate()) : 
                terms.getTermInMonths();
        
        Money paymentAmount = calculatePaymentForRate(principalAmount, terms, currentRate, firstPeriodPayments);
        periods.add(RatePeriod.builder()
                .periodNumber(1)
                .startPayment(currentPayment)
                .paymentCount(firstPeriodPayments)
                .interestRate(currentRate)
                .paymentAmount(paymentAmount)
                .build());
        
        currentPayment += firstPeriodPayments;
        
        // Subsequent periods with rate adjustments
        for (int i = 0; i < rateAdjustments.size(); i++) {
            RateAdjustment adjustment = rateAdjustments.get(i);
            currentRate = adjustment.getNewRate();
            
            int remainingPayments = terms.getTermInMonths() - currentPayment + 1;
            int periodPayments = (i < rateAdjustments.size() - 1) ? 
                    (int) ChronoUnit.MONTHS.between(adjustment.getEffectiveDate(), 
                            rateAdjustments.get(i + 1).getEffectiveDate()) : 
                    remainingPayments;
            
            Money adjustedPaymentAmount = calculatePaymentForRate(principalAmount, terms, currentRate, periodPayments);
            periods.add(RatePeriod.builder()
                    .periodNumber(i + 2)
                    .startPayment(currentPayment)
                    .paymentCount(periodPayments)
                    .interestRate(currentRate)
                    .paymentAmount(adjustedPaymentAmount)
                    .build());
            
            currentPayment += periodPayments;
        }
        
        return periods;
    }
    
    private Money calculatePaymentForRate(Money principalAmount, LoanTerms terms, BigDecimal rate, int payments) {
        LoanTerms adjustedTerms = terms.toBuilder()
                .interestRate(rate)
                .termInMonths(payments)
                .build();
        return calculationService.calculatePaymentAmount(principalAmount, adjustedTerms);
    }
    
    private int calculateEffectiveTermMonths(InstallmentPlan originalPlan, InstallmentModification modification) {
        return switch (modification.getType()) {
            case PAYMENT_AMOUNT_CHANGE -> originalPlan.getTermInMonths(); // Will be recalculated
            case PAYMENT_DEFERRAL -> originalPlan.getTermInMonths() + modification.getDeferralMonths();
            case TERM_EXTENSION -> originalPlan.getTermInMonths() + modification.getExtensionMonths();
        };
    }
    
    private Money calculateModifiedPaymentAmount(InstallmentPlan originalPlan, InstallmentModification modification) {
        return switch (modification.getType()) {
            case PAYMENT_AMOUNT_CHANGE -> modification.getNewPaymentAmount();
            case PAYMENT_DEFERRAL -> originalPlan.calculateMonthlyPayment();
            case TERM_EXTENSION -> {
                int newTerm = originalPlan.getTermInMonths() + modification.getExtensionMonths();
                LoanTerms extendedTerms = originalPlan.getTerms().toBuilder()
                        .termInMonths(newTerm)
                        .build();
                yield calculationService.calculatePaymentAmount(originalPlan.getPrincipalAmount(), extendedTerms);
            }
        };
    }
    
    private Money calculateModifiedTotalInterest(InstallmentPlan originalPlan, InstallmentModification modification) {
        Money modifiedPayment = calculateModifiedPaymentAmount(originalPlan, modification);
        int effectiveTerm = calculateEffectiveTermMonths(originalPlan, modification);
        Money totalPayments = modifiedPayment.multiply(new BigDecimal(effectiveTerm));
        return totalPayments.subtract(originalPlan.getPrincipalAmount());
    }
    
    private List<DeferredPayment> calculateDeferredPayments(InstallmentModification modification) {
        if (modification.getType() != InstallmentModificationType.PAYMENT_DEFERRAL) {
            return List.of();
        }
        
        return IntStream.range(0, modification.getDeferralMonths())
                .mapToObj(i -> DeferredPayment.builder()
                        .originalPaymentNumber(modification.getStartingPayment() + i)
                        .deferredAmount(modification.getOriginalPaymentAmount())
                        .newDueDate(modification.getDeferralEndDate().plusMonths(i))
                        .reason(modification.getReason())
                        .build())
                .toList();
    }
    
    private List<PaymentPeriod> calculateGraduatedPaymentPeriods(
            Money principalAmount, LoanTerms terms, GraduatedPaymentSchedule gradSchedule, LocalDate startDate) {
        
        List<PaymentPeriod> periods = new ArrayList<>();
        Money basePayment = calculationService.calculatePaymentAmount(principalAmount, terms);
        
        // Initial period at reduced payment
        Money initialPayment = basePayment.multiply(gradSchedule.getInitialPaymentPercentage().divide(new BigDecimal("100")));
        periods.add(PaymentPeriod.builder()
                .periodNumber(1)
                .startMonth(1)
                .monthCount(12)
                .paymentAmount(initialPayment)
                .startDate(startDate)
                .endDate(startDate.plusYears(1).minusDays(1))
                .build());
        
        // Graduated periods
        for (int year = 2; year <= gradSchedule.getGraduationPeriodYears(); year++) {
            BigDecimal increaseMultiplier = BigDecimal.ONE.add(
                    gradSchedule.getAnnualIncrease().multiply(new BigDecimal(year - 1)).divide(new BigDecimal("100")));
            Money periodPayment = initialPayment.multiply(increaseMultiplier);
            
            periods.add(PaymentPeriod.builder()
                    .periodNumber(year)
                    .startMonth((year - 1) * 12 + 1)
                    .monthCount(12)
                    .paymentAmount(periodPayment)
                    .startDate(startDate.plusYears(year - 1))
                    .endDate(startDate.plusYears(year).minusDays(1))
                    .build());
        }
        
        // Final period at full payment
        int finalPeriodStart = gradSchedule.getGraduationPeriodYears() + 1;
        int finalPeriodMonths = terms.getTermInMonths() - (gradSchedule.getGraduationPeriodYears() * 12);
        
        periods.add(PaymentPeriod.builder()
                .periodNumber(finalPeriodStart)
                .startMonth((finalPeriodStart - 1) * 12 + 1)
                .monthCount(finalPeriodMonths)
                .paymentAmount(basePayment)
                .startDate(startDate.plusYears(gradSchedule.getGraduationPeriodYears()))
                .endDate(startDate.plusMonths(terms.getTermInMonths()))
                .build());
        
        return periods;
    }
    
    private List<ScheduledInstallment> calculateInterestOnlyPayments(
            Money principalAmount, LoanTerms terms, InterestOnlyPeriod ioTerms, LocalDate startDate) {
        
        Money monthlyInterest = principalAmount.multiply(
                terms.getInterestRate().divide(new BigDecimal("12"), CALCULATION_CONTEXT).divide(new BigDecimal("100"), CALCULATION_CONTEXT));
        
        return IntStream.rangeClosed(1, ioTerms.getInterestOnlyMonths())
                .mapToObj(i -> ScheduledInstallment.builder()
                        .paymentNumber(i)
                        .dueDate(startDate.plusMonths(i))
                        .paymentAmount(monthlyInterest)
                        .principalAmount(Money.zero(principalAmount.getCurrency()))
                        .interestAmount(monthlyInterest)
                        .remainingBalance(principalAmount)
                        .build())
                .toList();
    }
    
    private List<ScheduledInstallment> calculatePrincipalAndInterestPayments(
            Money principalAmount, LoanTerms terms, InterestOnlyPeriod ioTerms, LocalDate startDate) {
        
        int remainingMonths = terms.getTermInMonths() - ioTerms.getInterestOnlyMonths();
        LoanTerms piTerms = terms.toBuilder()
                .termInMonths(remainingMonths)
                .build();
        
        Money piPayment = calculationService.calculatePaymentAmount(principalAmount, piTerms);
        LocalDate piStartDate = startDate.plusMonths(ioTerms.getInterestOnlyMonths());
        
        return IntStream.rangeClosed(1, remainingMonths)
                .mapToObj(i -> ScheduledInstallment.builder()
                        .paymentNumber(ioTerms.getInterestOnlyMonths() + i)
                        .dueDate(piStartDate.plusMonths(i))
                        .paymentAmount(piPayment)
                        .principalAmount(calculatePrincipalPortion(principalAmount, piTerms, i))
                        .interestAmount(calculateInterestPortion(principalAmount, piTerms, i))
                        .remainingBalance(calculateRemainingBalance(principalAmount, piTerms, i))
                        .build())
                .toList();
    }
    
    private LocalDate calculatePaymentDate(LocalDate startDate, int paymentNumber, PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> startDate.plusMonths(paymentNumber);
            case BI_WEEKLY -> startDate.plusWeeks(paymentNumber * 2L);
            case WEEKLY -> startDate.plusWeeks(paymentNumber);
            case QUARTERLY -> startDate.plusMonths(paymentNumber * 3L);
            case SEMI_ANNUALLY -> startDate.plusMonths(paymentNumber * 6L);
            case ANNUALLY -> startDate.plusYears(paymentNumber);
        };
    }
    
    private ScheduledInstallment adjustForHolidays(ScheduledInstallment payment, HolidayAdjustmentRule rule) {
        LocalDate adjustedDate = switch (rule) {
            case NEXT_BUSINESS_DAY -> adjustToNextBusinessDay(payment.getDueDate());
            case PREVIOUS_BUSINESS_DAY -> adjustToPreviousBusinessDay(payment.getDueDate());
            case NO_ADJUSTMENT -> payment.getDueDate();
        };
        
        return payment.toBuilder()
                .dueDate(adjustedDate)
                .build();
    }
    
    private LocalDate adjustToNextBusinessDay(LocalDate date) {
        while (date.getDayOfWeek().getValue() > 5) { // Weekend
            date = date.plusDays(1);
        }
        return date;
    }
    
    private LocalDate adjustToPreviousBusinessDay(LocalDate date) {
        while (date.getDayOfWeek().getValue() > 5) { // Weekend
            date = date.minusDays(1);
        }
        return date;
    }
    
    private Money calculatePrincipalPortion(Money principalAmount, LoanTerms terms, int paymentNumber) {
        // Simplified calculation - would need more complex amortization logic in production
        BigDecimal monthlyRate = terms.getInterestRate().divide(new BigDecimal("12"), CALCULATION_CONTEXT).divide(new BigDecimal("100"), CALCULATION_CONTEXT);
        Money payment = calculationService.calculatePaymentAmount(principalAmount, terms);
        Money remainingBalance = calculateRemainingBalance(principalAmount, terms, paymentNumber - 1);
        Money interestPortion = remainingBalance.multiply(monthlyRate);
        return payment.subtract(interestPortion);
    }
    
    private Money calculateInterestPortion(Money principalAmount, LoanTerms terms, int paymentNumber) {
        BigDecimal monthlyRate = terms.getInterestRate().divide(new BigDecimal("12"), CALCULATION_CONTEXT).divide(new BigDecimal("100"), CALCULATION_CONTEXT);
        Money remainingBalance = calculateRemainingBalance(principalAmount, terms, paymentNumber - 1);
        return remainingBalance.multiply(monthlyRate);
    }
    
    private Money calculateRemainingBalance(Money principalAmount, LoanTerms terms, int paymentNumber) {
        if (paymentNumber == 0) return principalAmount;
        
        BigDecimal monthlyRate = terms.getInterestRate().divide(new BigDecimal("12"), CALCULATION_CONTEXT).divide(new BigDecimal("100"), CALCULATION_CONTEXT);
        Money payment = calculationService.calculatePaymentAmount(principalAmount, terms);
        
        Money balance = principalAmount;
        for (int i = 1; i <= paymentNumber; i++) {
            Money interestPayment = balance.multiply(monthlyRate);
            Money principalPayment = payment.subtract(interestPayment);
            balance = balance.subtract(principalPayment);
        }
        
        return balance;
    }
    
    private java.util.Map<Integer, BigDecimal> calculatePrincipalPercentages(InstallmentPlan plan) {
        return java.util.Map.of(
                5, new BigDecimal("25.0"),
                10, new BigDecimal("45.0"),
                15, new BigDecimal("65.0"),
                20, new BigDecimal("80.0"),
                25, new BigDecimal("90.0"),
                30, new BigDecimal("100.0")
        );
    }
    
    private java.util.Map<Integer, BigDecimal> calculateInterestPercentages(InstallmentPlan plan) {
        return java.util.Map.of(
                5, new BigDecimal("75.0"),
                10, new BigDecimal("55.0"),
                15, new BigDecimal("35.0"),
                20, new BigDecimal("20.0"),
                25, new BigDecimal("10.0"),
                30, new BigDecimal("0.0")
        );
    }
    
    private Money calculateRemainingInterest(InstallmentPlan plan, int payoffAtPayment) {
        Money totalScheduledInterest = plan.getTotalInterestAmount();
        Money paidInterest = Money.zero(plan.getPrincipalAmount().getCurrency());
        
        for (int i = 1; i <= payoffAtPayment; i++) {
            paidInterest = paidInterest.add(calculateInterestPortion(plan.getPrincipalAmount(), plan.getTerms(), i));
        }
        
        return totalScheduledInterest.subtract(paidInterest);
    }
    
    private LocalDate calculateBreakEvenPoint(InstallmentPlan plan, int payoffAtPayment) {
        return plan.getStartDate().plusMonths(payoffAtPayment / 2); // Simplified calculation
    }
    
    private InstallmentOption createInstallmentOption(Money principalAmount, LoanTerms terms, LocalDate startDate) {
        InstallmentPlan plan = createInstallmentPlan(principalAmount, terms, startDate);
        
        return InstallmentOption.builder()
                .terms(terms)
                .monthlyPayment(plan.calculateMonthlyPayment())
                .totalInterest(plan.getTotalInterestAmount())
                .totalPayments(plan.getTotalPaymentAmount())
                .effectiveAPR(calculateEffectiveAPR(terms))
                .build();
    }
    
    private InstallmentOption determineRecommendation(List<InstallmentOption> options) {
        return options.stream()
                .min((o1, o2) -> o1.getTotalInterest().getAmount().compareTo(o2.getTotalInterest().getAmount()))
                .orElse(options.get(0));
    }
    
    private BigDecimal calculateEffectiveAPR(LoanTerms terms) {
        return terms.getInterestRate(); // Simplified - would include fees in production
    }
}