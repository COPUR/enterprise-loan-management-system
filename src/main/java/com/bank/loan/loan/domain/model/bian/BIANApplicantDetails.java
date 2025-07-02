package com.bank.loanmanagement.loan.domain.model.bian;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@Builder
public class BIANApplicantDetails {
    private String applicantId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String nationalId;
    private String phoneNumber;
    private String emailAddress;
    private String employmentStatus;
    private BigDecimal monthlyIncome;
    private String creditHistory;
    private String residentialAddress;
}