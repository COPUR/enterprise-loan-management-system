package com.bank.loanmanagement.infrastructure.repository;

import com.bank.loanmanagement.domain.customer.CreditCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCustomerRepository extends JpaRepository<CreditCustomer, Long> {
}