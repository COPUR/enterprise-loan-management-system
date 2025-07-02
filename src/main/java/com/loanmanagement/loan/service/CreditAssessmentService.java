// domain/service/CreditAssessmentService.java
package com.loanmanagement.loan.service;

import com.loanmanagement.domain.model.entity.Customer;
import com.loanmanagement.domain.model.value.Money;
import com.loanmanagement.domain.exception.InsufficientCreditException;
import org.springframework.stereotype.Service;

@Service
public class CreditAssessmentService {
    
    public void validateCreditEligibility(Customer customer, Money requestedAmount) {
        if (!customer.hasAvailableCreditFor(requestedAmount)) {
            throw new InsufficientCreditException(
                String.format("Customer %s %s has insufficient credit. Requested: %s, Available: %s",
                    customer.getName(),
                    customer.getSurname(),
                    requestedAmount.getValue(),
                    customer.getAvailableCredit().getValue())
            );
        }
    }
}