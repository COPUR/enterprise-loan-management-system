package com.loanmanagement.application.usecase;

import com.loanmanagement.application.dto.PayLoanRequest;
import com.loanmanagement.application.dto.PayLoanResponse;

public interface PayLoanUseCase {
    PayLoanResponse execute(PayLoanRequest request);
}