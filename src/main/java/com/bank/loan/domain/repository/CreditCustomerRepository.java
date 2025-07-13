package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.CreditCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for CreditCustomer entity
 */
@Repository
public interface CreditCustomerRepository extends JpaRepository<CreditCustomer, Long> {
    
    /**
     * Find customer by name and surname
     */
    Optional<CreditCustomer> findByNameAndSurname(String name, String surname);
}