package com.loanmanagement.payment.domain.service;

import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain Service for Payment Allocation
 * Handles payment allocation logic across different loan components
 */
@Slf4j
@Service
public class PaymentAllocationService {

    /**
     * Allocate payment according to the specified strategy
     */
    public PaymentAllocation allocatePayment(Money paymentAmount, LoanBalance loanBalance, 
                                           PaymentAllocationStrategy strategy) {
        log.debug("Allocating payment of {} using strategy: {}", paymentAmount, strategy);
        
        return switch (strategy) {
            case STANDARD -> allocateStandardPayment(paymentAmount, loanBalance);
            case FEES_FIRST -> allocateFeesFirstPayment(paymentAmount, loanBalance);
            case PRINCIPAL_FIRST -> allocatePrincipalFirstPayment(paymentAmount, loanBalance);
            case INTEREST_FIRST -> allocateInterestFirstPayment(paymentAmount, loanBalance);
            case WITH_ESCROW -> allocatePaymentWithEscrow(paymentAmount, loanBalance);
            case PROPORTIONAL -> allocateProportionalPayment(paymentAmount, loanBalance);
            case AVALANCHE -> allocateAvalanchePayment(paymentAmount, loanBalance);
            case SNOWBALL -> allocateSnowballPayment(paymentAmount, loanBalance);
            default -> allocateStandardPayment(paymentAmount, loanBalance);
        };
    }
    
    /**
     * Standard allocation: Fees -> Interest -> Principal
     */
    private PaymentAllocation allocateStandardPayment(Money paymentAmount, LoanBalance loanBalance) {
        Money remainingAmount = paymentAmount;
        String currency = paymentAmount.getCurrency();
        
        // Allocate to fees first (late fees, then other fees)
        Money lateFeesAmount = Money.min(remainingAmount, loanBalance.getLateFeesBalance());
        remainingAmount = remainingAmount.subtract(lateFeesAmount);
        
        Money otherFeesAmount = Money.min(remainingAmount, loanBalance.getOtherFeesBalance());
        remainingAmount = remainingAmount.subtract(otherFeesAmount);
        
        Money totalFeesAmount = lateFeesAmount.add(otherFeesAmount);
        
        // Allocate to interest
        Money interestAmount = Money.min(remainingAmount, loanBalance.getInterestBalance());
        remainingAmount = remainingAmount.subtract(interestAmount);
        
        // Remaining goes to principal
        Money principalAmount = Money.min(remainingAmount, loanBalance.getPrincipalBalance());
        Money overpaymentAmount = remainingAmount.subtract(principalAmount);
        
        List<String> allocationOrder = List.of("LATE_FEES", "OTHER_FEES", "INTEREST", "PRINCIPAL");
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .feesAmount(totalFeesAmount)
                .lateFeesAmount(lateFeesAmount)
                .otherFeesAmount(otherFeesAmount)
                .overpaymentAmount(overpaymentAmount)
                .escrowAmount(Money.zero(currency))
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.STANDARD)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    /**
     * Fees first allocation: All fees -> Interest -> Principal
     */
    private PaymentAllocation allocateFeesFirstPayment(Money paymentAmount, LoanBalance loanBalance) {
        Money remainingAmount = paymentAmount;
        String currency = paymentAmount.getCurrency();
        
        // Calculate total fees
        Money totalFeesBalance = loanBalance.getLateFeesBalance()
                .add(loanBalance.getOtherFeesBalance());
        
        // Allocate all available amount to fees first
        Money totalFeesAmount = Money.min(remainingAmount, totalFeesBalance);
        remainingAmount = remainingAmount.subtract(totalFeesAmount);
        
        // Split fees allocation
        Money lateFeesAmount = Money.min(totalFeesAmount, loanBalance.getLateFeesBalance());
        Money otherFeesAmount = totalFeesAmount.subtract(lateFeesAmount);
        
        // Allocate to interest
        Money interestAmount = Money.min(remainingAmount, loanBalance.getInterestBalance());
        remainingAmount = remainingAmount.subtract(interestAmount);
        
        // Remaining goes to principal
        Money principalAmount = Money.min(remainingAmount, loanBalance.getPrincipalBalance());
        Money overpaymentAmount = remainingAmount.subtract(principalAmount);
        
        List<String> allocationOrder = List.of("ALL_FEES", "INTEREST", "PRINCIPAL");
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .feesAmount(totalFeesAmount)
                .lateFeesAmount(lateFeesAmount)
                .otherFeesAmount(otherFeesAmount)
                .overpaymentAmount(overpaymentAmount)
                .escrowAmount(Money.zero(currency))
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.FEES_FIRST)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    /**
     * Principal first allocation: Principal -> Interest -> Fees
     */
    private PaymentAllocation allocatePrincipalFirstPayment(Money paymentAmount, LoanBalance loanBalance) {
        Money remainingAmount = paymentAmount;
        String currency = paymentAmount.getCurrency();
        
        // Allocate to principal first
        Money principalAmount = Money.min(remainingAmount, loanBalance.getPrincipalBalance());
        remainingAmount = remainingAmount.subtract(principalAmount);
        
        // Allocate to interest
        Money interestAmount = Money.min(remainingAmount, loanBalance.getInterestBalance());
        remainingAmount = remainingAmount.subtract(interestAmount);
        
        // Allocate remaining to fees
        Money lateFeesAmount = Money.min(remainingAmount, loanBalance.getLateFeesBalance());
        remainingAmount = remainingAmount.subtract(lateFeesAmount);
        
        Money otherFeesAmount = Money.min(remainingAmount, loanBalance.getOtherFeesBalance());
        Money overpaymentAmount = remainingAmount.subtract(otherFeesAmount);
        
        Money totalFeesAmount = lateFeesAmount.add(otherFeesAmount);
        
        List<String> allocationOrder = List.of("PRINCIPAL", "INTEREST", "LATE_FEES", "OTHER_FEES");
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .feesAmount(totalFeesAmount)
                .lateFeesAmount(lateFeesAmount)
                .otherFeesAmount(otherFeesAmount)
                .overpaymentAmount(overpaymentAmount)
                .escrowAmount(Money.zero(currency))
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.PRINCIPAL_FIRST)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    /**
     * Interest first allocation: Interest -> Fees -> Principal
     */
    private PaymentAllocation allocateInterestFirstPayment(Money paymentAmount, LoanBalance loanBalance) {
        Money remainingAmount = paymentAmount;
        String currency = paymentAmount.getCurrency();
        
        // Allocate to interest first
        Money interestAmount = Money.min(remainingAmount, loanBalance.getInterestBalance());
        remainingAmount = remainingAmount.subtract(interestAmount);
        
        // Allocate to fees
        Money lateFeesAmount = Money.min(remainingAmount, loanBalance.getLateFeesBalance());
        remainingAmount = remainingAmount.subtract(lateFeesAmount);
        
        Money otherFeesAmount = Money.min(remainingAmount, loanBalance.getOtherFeesBalance());
        remainingAmount = remainingAmount.subtract(otherFeesAmount);
        
        Money totalFeesAmount = lateFeesAmount.add(otherFeesAmount);
        
        // Remaining goes to principal
        Money principalAmount = Money.min(remainingAmount, loanBalance.getPrincipalBalance());
        Money overpaymentAmount = remainingAmount.subtract(principalAmount);
        
        List<String> allocationOrder = List.of("INTEREST", "LATE_FEES", "OTHER_FEES", "PRINCIPAL");
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .feesAmount(totalFeesAmount)
                .lateFeesAmount(lateFeesAmount)
                .otherFeesAmount(otherFeesAmount)
                .overpaymentAmount(overpaymentAmount)
                .escrowAmount(Money.zero(currency))
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.INTEREST_FIRST)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    /**
     * Payment allocation with escrow
     */
    private PaymentAllocation allocatePaymentWithEscrow(Money paymentAmount, LoanBalance loanBalance) {
        // First, separate escrow portion
        Money monthlyEscrowAmount = calculateMonthlyEscrowAmount(loanBalance);
        Money escrowAmount = Money.min(paymentAmount, monthlyEscrowAmount);
        Money remainingForLoan = paymentAmount.subtract(escrowAmount);
        
        // Apply standard allocation to the non-escrow portion
        PaymentAllocation loanAllocation = allocateStandardPayment(remainingForLoan, loanBalance);
        
        // Create escrow details
        EscrowDetails escrowDetails = EscrowDetails.builder()
                .propertyTaxes(escrowAmount.multiply(new BigDecimal("0.6"))) // 60% for property taxes
                .insurance(escrowAmount.multiply(new BigDecimal("0.4"))) // 40% for insurance
                .build();
        
        List<String> allocationOrder = new ArrayList<>(List.of("ESCROW"));
        allocationOrder.addAll(loanAllocation.getAllocationOrder());
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(loanAllocation.getPrincipalAmount())
                .interestAmount(loanAllocation.getInterestAmount())
                .feesAmount(loanAllocation.getFeesAmount())
                .lateFeesAmount(loanAllocation.getLateFeesAmount())
                .otherFeesAmount(loanAllocation.getOtherFeesAmount())
                .escrowAmount(escrowAmount)
                .overpaymentAmount(loanAllocation.getOverpaymentAmount())
                .includesEscrow(true)
                .escrowDetails(escrowDetails)
                .allocationStrategy(PaymentAllocationStrategy.WITH_ESCROW)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    /**
     * Proportional allocation based on outstanding balances
     */
    private PaymentAllocation allocateProportionalPayment(Money paymentAmount, LoanBalance loanBalance) {
        Money totalOutstanding = loanBalance.getTotalBalance();
        String currency = paymentAmount.getCurrency();
        
        if (totalOutstanding.isZero()) {
            return createZeroAllocation(paymentAmount);
        }
        
        // Calculate proportions
        BigDecimal principalProportion = loanBalance.getPrincipalBalance().getAmount()
                .divide(totalOutstanding.getAmount(), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal interestProportion = loanBalance.getInterestBalance().getAmount()
                .divide(totalOutstanding.getAmount(), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal feesProportion = loanBalance.getFeesBalance().getAmount()
                .divide(totalOutstanding.getAmount(), 4, java.math.RoundingMode.HALF_UP);
        
        // Allocate proportionally
        Money principalAmount = paymentAmount.multiply(principalProportion);
        Money interestAmount = paymentAmount.multiply(interestProportion);
        Money feesAmount = paymentAmount.multiply(feesProportion);
        
        // Handle rounding differences
        Money allocatedTotal = principalAmount.add(interestAmount).add(feesAmount);
        Money difference = paymentAmount.subtract(allocatedTotal);
        principalAmount = principalAmount.add(difference); // Add any rounding difference to principal
        
        List<String> allocationOrder = List.of("PROPORTIONAL_ALL");
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .feesAmount(feesAmount)
                .lateFeesAmount(Money.zero(currency))
                .otherFeesAmount(feesAmount)
                .escrowAmount(Money.zero(currency))
                .overpaymentAmount(Money.zero(currency))
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.PROPORTIONAL)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    /**
     * Avalanche allocation: Highest interest rate components first
     */
    private PaymentAllocation allocateAvalanchePayment(Money paymentAmount, LoanBalance loanBalance) {
        // For simplicity, assume late fees have highest "rate", then interest, then principal
        return allocateFeesFirstPayment(paymentAmount, loanBalance).toBuilder()
                .allocationStrategy(PaymentAllocationStrategy.AVALANCHE)
                .build();
    }
    
    /**
     * Snowball allocation: Smallest balances first
     */
    private PaymentAllocation allocateSnowballPayment(Money paymentAmount, LoanBalance loanBalance) {
        Money remainingAmount = paymentAmount;
        String currency = paymentAmount.getCurrency();
        
        // Sort components by balance size (smallest first)
        List<BalanceComponent> components = getSortedBalanceComponents(loanBalance);
        
        Money principalAmount = Money.zero(currency);
        Money interestAmount = Money.zero(currency);
        Money lateFeesAmount = Money.zero(currency);
        Money otherFeesAmount = Money.zero(currency);
        
        List<String> allocationOrder = new ArrayList<>();
        
        for (BalanceComponent component : components) {
            if (remainingAmount.isZero()) break;
            
            Money componentBalance = component.getBalance();
            Money allocation = Money.min(remainingAmount, componentBalance);
            remainingAmount = remainingAmount.subtract(allocation);
            
            switch (component.getType()) {
                case "PRINCIPAL" -> principalAmount = allocation;
                case "INTEREST" -> interestAmount = allocation;
                case "LATE_FEES" -> lateFeesAmount = allocation;
                case "OTHER_FEES" -> otherFeesAmount = allocation;
            }
            
            allocationOrder.add(component.getType());
        }
        
        Money totalFeesAmount = lateFeesAmount.add(otherFeesAmount);
        Money overpaymentAmount = remainingAmount;
        
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .feesAmount(totalFeesAmount)
                .lateFeesAmount(lateFeesAmount)
                .otherFeesAmount(otherFeesAmount)
                .escrowAmount(Money.zero(currency))
                .overpaymentAmount(overpaymentAmount)
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.SNOWBALL)
                .allocationOrder(allocationOrder)
                .build();
    }
    
    // Helper methods
    
    private Money calculateMonthlyEscrowAmount(LoanBalance loanBalance) {
        // Default escrow calculation - in real implementation, this would be configurable
        return Money.of(loanBalance.getPrincipalBalance().getCurrency(), new BigDecimal("300.00"));
    }
    
    private PaymentAllocation createZeroAllocation(Money paymentAmount) {
        String currency = paymentAmount.getCurrency();
        return PaymentAllocation.builder()
                .totalAmount(paymentAmount)
                .principalAmount(Money.zero(currency))
                .interestAmount(Money.zero(currency))
                .feesAmount(Money.zero(currency))
                .lateFeesAmount(Money.zero(currency))
                .otherFeesAmount(Money.zero(currency))
                .escrowAmount(Money.zero(currency))
                .overpaymentAmount(paymentAmount)
                .includesEscrow(false)
                .allocationStrategy(PaymentAllocationStrategy.STANDARD)
                .allocationOrder(List.of("OVERPAYMENT"))
                .build();
    }
    
    private List<BalanceComponent> getSortedBalanceComponents(LoanBalance loanBalance) {
        List<BalanceComponent> components = new ArrayList<>();
        
        components.add(new BalanceComponent("PRINCIPAL", loanBalance.getPrincipalBalance()));
        components.add(new BalanceComponent("INTEREST", loanBalance.getInterestBalance()));
        components.add(new BalanceComponent("LATE_FEES", loanBalance.getLateFeesBalance()));
        components.add(new BalanceComponent("OTHER_FEES", loanBalance.getOtherFeesBalance()));
        
        // Sort by balance amount (smallest first for snowball)
        components.sort((c1, c2) -> c1.getBalance().getAmount().compareTo(c2.getBalance().getAmount()));
        
        return components;
    }
    
    // Helper class for balance component sorting
    private static class BalanceComponent {
        private final String type;
        private final Money balance;
        
        public BalanceComponent(String type, Money balance) {
            this.type = type;
            this.balance = balance;
        }
        
        public String getType() { return type; }
        public Money getBalance() { return balance; }
    }
}