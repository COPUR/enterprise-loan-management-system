package com.loanmanagement.application.usecase;

import com.loanmanagement.application.dto.LoanDto;
import java.util.List;

public interface ListLoansUseCase {
    List<LoanDto> execute(Long customerId, Integer numberOfInstallments, Boolean isPaid);
}
