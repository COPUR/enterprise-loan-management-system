package com.bank.loanmanagement.loan.infrastructure.repository;

import com.bank.loanmanagement.loan.domain.customer.CreditCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCustomerRepository extends JpaRepository<CreditCustomer, Long> {
}