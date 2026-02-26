package com.bank.loan.infrastructure.web;

import com.bank.loan.application.LoanManagementService;
import com.bank.loan.application.dto.CreateLoanRequest;
import com.bank.loan.application.dto.LoanResponse;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    @Mock
    private LoanManagementService loanService;

    @InjectMocks
    private LoanController controller;

    @Test
    void createLoanApplicationShouldReturnCreated() {
        CreateLoanRequest request = new CreateLoanRequest(
            "CUST-CTRL-001",
            new BigDecimal("10000.00"),
            "AED",
            new BigDecimal("6.5"),
            24
        );
        LoanResponse response = sampleResponse("LOAN-CTRL-001", "CREATED");
        when(loanService.createLoanApplication(request)).thenReturn(response);

        ResponseEntity<LoanResponse> entity = controller.createLoanApplication(request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getBody()).isEqualTo(response);
    }

    @Test
    void getApproveDisburseShouldReturnOkResponses() {
        when(loanService.findLoanById("LOAN-CTRL-002")).thenReturn(sampleResponse("LOAN-CTRL-002", "CREATED"));
        when(loanService.approveLoan("LOAN-CTRL-002")).thenReturn(sampleResponse("LOAN-CTRL-002", "APPROVED"));
        when(loanService.disburseLoan("LOAN-CTRL-002")).thenReturn(sampleResponse("LOAN-CTRL-002", "DISBURSED"));

        assertThat(controller.getLoan("LOAN-CTRL-002").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.approveLoan("LOAN-CTRL-002").getBody().status()).isEqualTo("APPROVED");
        assertThat(controller.disburseLoan("LOAN-CTRL-002").getBody().status()).isEqualTo("DISBURSED");
    }

    @Test
    void rejectAndCancelShouldDelegateReason() {
        when(loanService.rejectLoan("LOAN-CTRL-003", "Policy")).thenReturn(sampleResponse("LOAN-CTRL-003", "REJECTED"));
        when(loanService.cancelLoan("LOAN-CTRL-003", "Customer request"))
            .thenReturn(sampleResponse("LOAN-CTRL-003", "CANCELLED"));

        ResponseEntity<LoanResponse> rejected = controller.rejectLoan(
            "LOAN-CTRL-003",
            new LoanController.RejectLoanRequest("Policy")
        );
        ResponseEntity<LoanResponse> cancelled = controller.cancelLoan(
            "LOAN-CTRL-003",
            new LoanController.CancelLoanRequest("Customer request")
        );

        assertThat(rejected.getBody().status()).isEqualTo("REJECTED");
        assertThat(cancelled.getBody().status()).isEqualTo("CANCELLED");
    }

    @Test
    void makePaymentShouldConvertMoneyAndDelegate() {
        when(loanService.makePayment(eq("LOAN-CTRL-004"), eq(Money.aed(new BigDecimal("750.00")))))
            .thenReturn(sampleResponse("LOAN-CTRL-004", "ACTIVE"));

        ResponseEntity<LoanResponse> entity = controller.makePayment(
            "LOAN-CTRL-004",
            new LoanController.MakePaymentRequest(new BigDecimal("750.00"), "AED")
        );

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().status()).isEqualTo("ACTIVE");

        ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);
        verify(loanService).makePayment(eq("LOAN-CTRL-004"), moneyCaptor.capture());
        assertThat(moneyCaptor.getValue().getAmount()).isEqualByComparingTo("750.00");
        assertThat(moneyCaptor.getValue().getCurrency().getCurrencyCode()).isEqualTo("AED");
    }

    private static LoanResponse sampleResponse(String loanId, String status) {
        return new LoanResponse(
            loanId,
            "CUST-CTRL-001",
            new BigDecimal("10000.00"),
            new BigDecimal("6.5"),
            24,
            status,
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2026-01-02"),
            LocalDate.parse("2026-01-03"),
            LocalDate.parse("2028-01-03"),
            new BigDecimal("9500.00"),
            new BigDecimal("445.12"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z")
        );
    }
}
