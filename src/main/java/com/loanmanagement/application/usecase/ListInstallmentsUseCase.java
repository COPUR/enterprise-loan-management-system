package com.loanmanagement.application.usecase;

import com.loanmanagement.application.dto.InstallmentDto;
import java.util.List;

public interface ListInstallmentsUseCase {
    List<InstallmentDto> execute(Long loanId);
}
