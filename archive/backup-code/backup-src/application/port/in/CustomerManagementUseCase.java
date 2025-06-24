
package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.application.dto.CustomerCreateRequest;
import com.bank.loanmanagement.application.dto.CustomerUpdateRequest;
import com.bank.loanmanagement.application.dto.CustomerResponse;
import com.bank.loanmanagement.domain.model.Customer;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerManagementUseCase {
    
    CustomerResponse createCustomer(CustomerCreateRequest request);
    
    CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request);
    
    CustomerResponse getCustomerById(Long customerId);
    
    CustomerResponse getCustomerByEmail(String email);
    
    List<CustomerResponse> getAllCustomers();
    
    List<CustomerResponse> getActiveCustomers();
    
    void suspendCustomer(Long customerId);
    
    void activateCustomer(Long customerId);
    
    void blockCustomer(Long customerId);
    
    boolean updateCreditLimit(Long customerId, BigDecimal newCreditLimit);
    
    boolean reserveCredit(Long customerId, BigDecimal amount);
    
    boolean releaseCredit(Long customerId, BigDecimal amount);
    
    BigDecimal getAvailableCredit(Long customerId);
    
    List<CustomerResponse> getCustomersWithMinimumCredit(BigDecimal minCredit);
    
    void deleteCustomer(Long customerId);
}
