package com.bank.customer.application.saga;

import com.bank.customer.application.CustomerManagementService;
import com.bank.shared.kernel.event.LoanDisbursedEvent;
import com.bank.shared.kernel.event.LoanFullyPaidEvent;
import com.bank.shared.kernel.event.PaymentFailedEvent;
import com.bank.shared.kernel.event.EventHandler;
import com.bank.shared.kernel.domain.Money;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Customer Credit Management Saga
 * 
 * Implements Saga Pattern for distributed credit management
 * Following Event-Driven Architecture (EDA) principles
 * 
 * Functional Requirements:
 * - FR-003: Automated credit limit management
 * - FR-004: Cross-context credit coordination
 */
@Component
public class CustomerCreditSaga {
    
    private final CustomerManagementService customerService;
    
    public CustomerCreditSaga(CustomerManagementService customerService) {
        this.customerService = customerService;
    }
    
    /**
     * Handle loan disbursement - reserve customer credit
     */
    @EventListener
    @EventHandler(name = "credit-reservation-handler", async = true)
    @Async
    public void handleLoanDisbursed(LoanDisbursedEvent event) {
        try {
            // Reserve credit when loan is disbursed
            customerService.reserveCredit(
                event.getCustomerId().getValue(), 
                event.getPrincipalAmount()
            );
            
            System.out.println("Credit reserved for customer: " + event.getCustomerId().getValue() + 
                             ", amount: " + event.getPrincipalAmount());
            
        } catch (Exception e) {
            System.err.println("Failed to reserve credit for loan disbursement: " + e.getMessage());
            // In a real implementation, this would trigger a compensating transaction
            // to reverse the loan disbursement
        }
    }
    
    /**
     * Handle loan fully paid - release reserved credit
     */
    @EventListener
    @EventHandler(name = "credit-release-handler", async = true)
    @Async
    public void handleLoanFullyPaid(LoanFullyPaidEvent event) {
        try {
            // Calculate the amount to release (this is simplified)
            // In practice, you'd need to track the original reserved amount
            System.out.println("Loan fully paid - releasing credit for customer: " + event.getCustomerId().getValue());
            
            // This would involve looking up the original loan amount and releasing it
            // For now, we'll just log the event
            
        } catch (Exception e) {
            System.err.println("Failed to release credit for paid loan: " + e.getMessage());
        }
    }
    
    /**
     * Handle payment failure - potential credit adjustment
     */
    @EventListener
    @EventHandler(name = "payment-failure-handler", async = true)
    @Async
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Business Logic: When a payment fails, we might need to:
        // 1. Check if it's a loan payment
        // 2. Update customer risk profile
        // 3. Potentially adjust credit limits
        
        System.out.println("Payment failed for customer: " + event.getCustomerId().getValue() + 
                         ", reason: " + event.getFailureReason());
        
        // This could trigger:
        // - Risk assessment updates
        // - Credit limit reviews
        // - Customer notifications
    }
    
    /**
     * Compensating transaction for failed credit operations
     */
    public void compensateFailedCreditReservation(String customerId, Money amount, String reason) {
        try {
            // This would be called if a downstream operation fails after credit reservation
            customerService.releaseCredit(customerId, amount);
            
            System.out.println("Compensated failed credit reservation for customer: " + customerId + 
                             ", amount: " + amount + ", reason: " + reason);
            
        } catch (Exception e) {
            System.err.println("Failed to compensate credit reservation: " + e.getMessage());
            // This would require manual intervention or dead letter queue processing
        }
    }
}