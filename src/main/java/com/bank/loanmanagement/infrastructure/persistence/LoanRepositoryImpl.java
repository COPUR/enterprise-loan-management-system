package com.bank.loanmanagement.infrastructure.persistence;

import com.bank.loanmanagement.domain.customer.CustomerId;
import com.bank.loanmanagement.domain.loan.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepositoryImpl implements LoanRepository {
    
    private final JpaLoanRepository jpaLoanRepository;
    
    @Autowired
    public LoanRepositoryImpl(JpaLoanRepository jpaLoanRepository) {
        this.jpaLoanRepository = jpaLoanRepository;
    }
    
    @Override
    public Loan save(Loan loan) {
        return jpaLoanRepository.save(loan);
    }
    
    @Override
    public Optional<Loan> findById(LoanId id) {
        return jpaLoanRepository.findById(id);
    }
    
    @Override
    public List<Loan> findByCustomerId(CustomerId customerId) {
        return jpaLoanRepository.findByCustomerId(customerId);
    }
    
    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        return jpaLoanRepository.findByStatus(status);
    }
    
    @Override
    public List<Loan> findByType(LoanType type) {
        return jpaLoanRepository.findByType(type);
    }
    
    @Override
    public List<Loan> findOverdueLoans() {
        return jpaLoanRepository.findOverdueLoans();
    }
    
    @Override
    public List<Loan> findAll() {
        return jpaLoanRepository.findAll();
    }
    
    @Override
    public void delete(Loan loan) {
        jpaLoanRepository.delete(loan);
    }
}