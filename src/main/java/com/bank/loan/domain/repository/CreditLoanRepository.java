package com.bank.loan.domain.repository;

import com.bank.loan.domain.model.CreditLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for CreditLoan entity
 */
@Repository
public interface CreditLoanRepository extends JpaRepository<CreditLoan, Long> {
    
    /**
     * Find loans by customer ID
     */
    List<CreditLoan> findByCustomerId(Long customerId);
    
    /**
     * Find loans by customer ID and number of installments
     */
    List<CreditLoan> findByCustomerIdAndNumberOfInstallments(Long customerId, Integer numberOfInstallments);
    
    /**
     * Find loans by customer ID and paid status
     */
    List<CreditLoan> findByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);
    
    /**
     * Find loans with filters
     */
    @Query("SELECT l FROM CreditLoan l WHERE " +
           "(:customerId IS NULL OR l.customerId = :customerId) AND " +
           "(:numberOfInstallments IS NULL OR l.numberOfInstallments = :numberOfInstallments) AND " +
           "(:isPaid IS NULL OR l.isPaid = :isPaid)")
    List<CreditLoan> findLoansWithFilters(
        @Param("customerId") Long customerId,
        @Param("numberOfInstallments") Integer numberOfInstallments,
        @Param("isPaid") Boolean isPaid
    );
}