// application/event/MetricsService.java
package com.loanmanagement.application.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter loansCreated;
    private final Counter paymentsProcessed;
    private final Counter paymentsFailed;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.loansCreated = Counter.builder("loans.created")
            .description("Number of loans created")
            .register(meterRegistry);
            
        this.paymentsProcessed = Counter.builder("payments.processed")
            .description("Number of payments processed")
            .register(meterRegistry);
            
        this.paymentsFailed = Counter.builder("payments.failed")
            .description("Number of failed payments")
            .register(meterRegistry);
    }
    
    public void recordLoanCreation(BigDecimal amount, Integer installments) {
        loansCreated.increment();
        
        meterRegistry.gauge("loan.amount", amount.doubleValue());
        meterRegistry.gauge("loan.installments", installments);
    }
    
    public void recordPayment(BigDecimal amount, BigDecimal discount, BigDecimal penalty, boolean fullyPaid) {
        paymentsProcessed.increment();
        
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            meterRegistry.summary("payment.discount").record(discount.doubleValue());
        }
        
        if (penalty.compareTo(BigDecimal.ZERO) > 0) {
            meterRegistry.summary("payment.penalty").record(penalty.doubleValue());
        }
        
        if (fullyPaid) {
            meterRegistry.counter("loans.fully.paid").increment();
        }
    }
    
    public void recordPaymentFailure(Long loanId) {
        paymentsFailed.increment();
    }
    
    public void updateCreditUtilization(Long customerId, BigDecimal availableCredit) {
        meterRegistry.gauge("credit.available", customerId, availableCredit.doubleValue());
    }
}
