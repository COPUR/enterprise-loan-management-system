package com.loanmanagement.customer.application.port.in;

import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.shared.domain.model.Money;
import java.time.LocalDate;

public interface CreateCustomerUseCase {
    
    Customer createCustomer(CreateCustomerCommand command);
    
    record CreateCustomerCommand(
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            Money monthlyIncome,
            Integer creditScore,
            String address,
            String occupation
    ) {}
}