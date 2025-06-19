
package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.application.dto.LoanApplicationRequest;
import com.bank.loanmanagement.application.dto.LoanResponse;
import com.bank.loanmanagement.application.dto.LoanApprovalRequest;
import com.bank.loanmanagement.domain.model.Loan;

import java.math.BigDecimal;
import java.util.List;

public interface LoanManagementUseCase {
    
    LoanResponse createLoanApplication(LoanApplicationRequest request);
    
    LoanResponse approveLoan(Long loanId, LoanApprovalRequest request);
    
    LoanResponse rejectLoan(Long loanId, String reason);
    
    LoanResponse activateLoan(Long loanId);
    
    LoanResponse getLoanById(Long loanId);
    
    List<LoanResponse> getLoansByCustomerId(Long customerId);
    
    List<LoanResponse> getAllLoans();
    
    List<LoanResponse> getLoansByStatus(Loan.LoanStatus status);
    
    List<LoanResponse> getActiveLoans();
    
    List<LoanResponse> getOverdueLoans();
    
    BigDecimal calculateMonthlyPayment(BigDecimal loanAmount, BigDecimal interestRate, Integer termInMonths);
    
    BigDecimal calculateTotalInterest(BigDecimal loanAmount, BigDecimal interestRate, Integer termInMonths);
    
    void generateLoanInstallments(Long loanId);
    
    boolean closeLoan(Long loanId);
    
    BigDecimal getTotalOutstandingBalance();
    
    BigDecimal getAverageInterestRate();
    
    Long getActiveLoanCount();
}
