package com.bank.loanmanagement.loan.infrastructure.persistence;

import com.bank.loanmanagement.loan.domain.customer.Customer;
import com.bank.loanmanagement.loan.domain.customer.CustomerId;
import com.bank.loanmanagement.loan.domain.customer.CustomerRepository;
import com.bank.loanmanagement.loan.domain.customer.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaCustomerRepository extends JpaRepository<Customer, CustomerId> {
    
    @Query("SELECT c FROM Customer c WHERE c.email = :email")
    Optional<Customer> findByEmail(@Param("email") String email);
    
    @Query("SELECT c FROM Customer c WHERE c.ssn = :ssn")
    Optional<Customer> findBySsn(@Param("ssn") String ssn);
    
    @Query("SELECT c FROM Customer c WHERE c.status = :status")
    List<Customer> findByStatus(@Param("status") CustomerStatus status);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.email = :email")
    boolean existsByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.ssn = :ssn")
    boolean existsBySsn(@Param("ssn") String ssn);
}