
package com.bank.loanmanagement.domain.repository;

import com.bank.loanmanagement.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    
    List<Customer> findByStatus(Customer.CustomerStatus status);
    
    @Query("SELECT c FROM Customer c WHERE c.availableCredit >= :minCredit")
    List<Customer> findByAvailableCreditGreaterThanEqual(@Param("minCredit") BigDecimal minCredit);
    
    @Query("SELECT c FROM Customer c WHERE c.creditLimit BETWEEN :minLimit AND :maxLimit")
    List<Customer> findByCreditLimitBetween(@Param("minLimit") BigDecimal minLimit, 
                                          @Param("maxLimit") BigDecimal maxLimit);
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status")
    Long countByStatus(@Param("status") Customer.CustomerStatus status);
    
    @Query("SELECT SUM(c.creditLimit) FROM Customer c WHERE c.status = 'ACTIVE'")
    BigDecimal getTotalActiveCreditLimit();
    
    @Query("SELECT SUM(c.availableCredit) FROM Customer c WHERE c.status = 'ACTIVE'")
    BigDecimal getTotalAvailableCredit();
    
    @Query("SELECT c FROM Customer c JOIN c.loans l WHERE l.status = 'ACTIVE' GROUP BY c HAVING COUNT(l) >= :minLoans")
    List<Customer> findCustomersWithMinimumActiveLoans(@Param("minLoans") Integer minLoans);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
}
