package com.bank.loan.infrastructure.external;

import com.bank.loan.application.CustomerCreditService;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for customer credit operations
 * 
 * Implements Hexagonal Architecture - Infrastructure adapter for cross-context calls
 * In production, this would integrate with the customer context via API or messaging
 */
@Component
public class CustomerCreditServiceAdapter implements CustomerCreditService {
    
    // Mock customer credit data for demonstration
    private final Map<String, CustomerCreditInfo> mockCustomerCredit = new HashMap<>();
    
    public CustomerCreditServiceAdapter() {
        // Initialize mock customer credit data
        mockCustomerCredit.put("CUST-12345678", new CustomerCreditInfo("CUST-12345678", BigDecimal.valueOf(100000), BigDecimal.ZERO));
        mockCustomerCredit.put("CUST-87654321", new CustomerCreditInfo("CUST-87654321", BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)));
        mockCustomerCredit.put("CUST-11111111", new CustomerCreditInfo("CUST-11111111", BigDecimal.valueOf(25000), BigDecimal.valueOf(20000)));
    }
    
    @Override
    public boolean hasAvailableCredit(CustomerId customerId, Money amount) {
        CustomerCreditInfo creditInfo = mockCustomerCredit.get(customerId.getValue());
        if (creditInfo == null) {
            return false;
        }
        
        BigDecimal availableCredit = creditInfo.getCreditLimit().subtract(creditInfo.getUsedCredit());
        return availableCredit.compareTo(amount.getAmount()) >= 0;
    }
    
    @Override
    public boolean reserveCredit(CustomerId customerId, Money amount) {
        CustomerCreditInfo creditInfo = mockCustomerCredit.get(customerId.getValue());
        if (creditInfo == null) {
            return false;
        }
        
        if (!hasAvailableCredit(customerId, amount)) {
            return false;
        }
        
        // Reserve credit by increasing used credit
        BigDecimal newUsedCredit = creditInfo.getUsedCredit().add(amount.getAmount());
        creditInfo.setUsedCredit(newUsedCredit);
        
        System.out.println("Reserved credit for customer: " + customerId.getValue() + 
                         ", amount: " + amount + 
                         ", new used credit: " + newUsedCredit);
        
        return true;
    }
    
    @Override
    public boolean releaseCredit(CustomerId customerId, Money amount) {
        CustomerCreditInfo creditInfo = mockCustomerCredit.get(customerId.getValue());
        if (creditInfo == null) {
            return false;
        }
        
        // Release credit by decreasing used credit
        BigDecimal newUsedCredit = creditInfo.getUsedCredit().subtract(amount.getAmount());
        if (newUsedCredit.compareTo(BigDecimal.ZERO) < 0) {
            newUsedCredit = BigDecimal.ZERO;
        }
        
        creditInfo.setUsedCredit(newUsedCredit);
        
        System.out.println("Released credit for customer: " + customerId.getValue() + 
                         ", amount: " + amount + 
                         ", new used credit: " + newUsedCredit);
        
        return true;
    }
    
    @Override
    public Money getAvailableCredit(CustomerId customerId) {
        CustomerCreditInfo creditInfo = mockCustomerCredit.get(customerId.getValue());
        if (creditInfo == null) {
            return Money.zero(Currency.getInstance("USD"));
        }
        
        BigDecimal availableCredit = creditInfo.getCreditLimit().subtract(creditInfo.getUsedCredit());
        return Money.usd(availableCredit);
    }
    
    /**
     * Mock customer credit information
     * In production, this would be replaced by actual customer service calls
     */
    private static class CustomerCreditInfo {
        private final String customerId;
        private final BigDecimal creditLimit;
        private BigDecimal usedCredit;
        
        public CustomerCreditInfo(String customerId, BigDecimal creditLimit, BigDecimal usedCredit) {
            this.customerId = customerId;
            this.creditLimit = creditLimit;
            this.usedCredit = usedCredit;
        }
        
        public String getCustomerId() { return customerId; }
        public BigDecimal getCreditLimit() { return creditLimit; }
        public BigDecimal getUsedCredit() { return usedCredit; }
        public void setUsedCredit(BigDecimal usedCredit) { this.usedCredit = usedCredit; }
    }
}