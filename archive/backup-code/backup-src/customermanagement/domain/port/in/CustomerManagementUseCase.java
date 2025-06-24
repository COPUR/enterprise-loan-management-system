
package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerManagementUseCase {
    Customer createCustomer(CreateCustomerCommand command);
    Customer updateCustomer(UpdateCustomerCommand command);
    Customer getCustomerById(Long customerId);
    List<Customer> getAllCustomers();
    void deleteCustomer(Long customerId);
    void reserveCredit(Long customerId, BigDecimal amount);
    void releaseCredit(Long customerId, BigDecimal amount);
}
