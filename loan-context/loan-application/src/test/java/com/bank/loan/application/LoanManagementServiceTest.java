package com.bank.loan.application;

import com.bank.loan.application.dto.CreateLoanRequest;
import com.bank.loan.application.dto.LoanResponse;
import com.bank.loan.domain.InterestRate;
import com.bank.loan.domain.Loan;
import com.bank.loan.domain.LoanId;
import com.bank.loan.domain.LoanRepository;
import com.bank.loan.domain.LoanTerm;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanManagementServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CustomerCreditService customerCreditService;

    @InjectMocks
    private LoanManagementService service;

    @Test
    void createLoanApplicationShouldPersistWhenCreditIsAvailable() {
        CreateLoanRequest request = new CreateLoanRequest(
            "CUST-LOAN-001",
            new BigDecimal("25000.00"),
            "AED",
            new BigDecimal("6.5"),
            24
        );
        when(customerCreditService.hasAvailableCredit(any(CustomerId.class), any(Money.class))).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponse response = service.createLoanApplication(request);

        assertThat(response.customerId()).isEqualTo("CUST-LOAN-001");
        assertThat(response.status()).isEqualTo("CREATED");
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void createLoanApplicationShouldRejectWhenCreditIsInsufficient() {
        CreateLoanRequest request = new CreateLoanRequest(
            "CUST-LOAN-002",
            new BigDecimal("25000.00"),
            "AED",
            new BigDecimal("6.5"),
            24
        );
        when(customerCreditService.hasAvailableCredit(any(CustomerId.class), any(Money.class))).thenReturn(false);
        when(customerCreditService.getAvailableCredit(any(CustomerId.class))).thenReturn(Money.aed(new BigDecimal("1000.00")));

        assertThatThrownBy(() -> service.createLoanApplication(request))
            .isInstanceOf(InsufficientCreditException.class)
            .hasMessageContaining("insufficient credit");

        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void approveRejectAndCancelShouldTransitionAndPersist() {
        Loan loanToApprove = loan("LOAN-SVC-001", "12000.00", 12);
        when(loanRepository.findById(LoanId.of("LOAN-SVC-001"))).thenReturn(Optional.of(loanToApprove));
        when(loanRepository.save(loanToApprove)).thenReturn(loanToApprove);

        LoanResponse approved = service.approveLoan("LOAN-SVC-001");
        assertThat(approved.status()).isEqualTo("APPROVED");

        Loan loanToReject = loan("LOAN-SVC-002", "12000.00", 12);
        when(loanRepository.findById(LoanId.of("LOAN-SVC-002"))).thenReturn(Optional.of(loanToReject));
        when(loanRepository.save(loanToReject)).thenReturn(loanToReject);
        LoanResponse rejected = service.rejectLoan("LOAN-SVC-002", "Policy");
        assertThat(rejected.status()).isEqualTo("REJECTED");

        Loan loanToCancel = loan("LOAN-SVC-003", "12000.00", 12);
        when(loanRepository.findById(LoanId.of("LOAN-SVC-003"))).thenReturn(Optional.of(loanToCancel));
        when(loanRepository.save(loanToCancel)).thenReturn(loanToCancel);
        LoanResponse cancelled = service.cancelLoan("LOAN-SVC-003", "Customer request");
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void disburseShouldReserveCreditAndPersist() {
        Loan approvedLoan = loan("LOAN-SVC-004", "10000.00", 12);
        approvedLoan.approve();
        when(loanRepository.findById(LoanId.of("LOAN-SVC-004"))).thenReturn(Optional.of(approvedLoan));
        when(loanRepository.save(approvedLoan)).thenReturn(approvedLoan);
        when(customerCreditService.reserveCredit(approvedLoan.getCustomerId(), approvedLoan.getPrincipalAmount())).thenReturn(true);

        LoanResponse response = service.disburseLoan("LOAN-SVC-004");

        assertThat(response.status()).isEqualTo("DISBURSED");
        verify(customerCreditService).reserveCredit(approvedLoan.getCustomerId(), approvedLoan.getPrincipalAmount());
        verify(loanRepository).save(approvedLoan);
    }

    @Test
    void makePaymentShouldReleaseCreditOnlyWhenLoanBecomesFullyPaid() {
        Loan fullPaymentLoan = loan("LOAN-SVC-005", "5000.00", 12);
        fullPaymentLoan.approve();
        fullPaymentLoan.disburse();
        when(loanRepository.findById(LoanId.of("LOAN-SVC-005"))).thenReturn(Optional.of(fullPaymentLoan));
        when(loanRepository.save(fullPaymentLoan)).thenReturn(fullPaymentLoan);

        LoanResponse fullyPaid = service.makePayment("LOAN-SVC-005", Money.aed(new BigDecimal("5000.00")));

        assertThat(fullyPaid.status()).isEqualTo("FULLY_PAID");
        verify(customerCreditService).releaseCredit(fullPaymentLoan.getCustomerId(), fullPaymentLoan.getPrincipalAmount());

        Loan partialPaymentLoan = loan("LOAN-SVC-006", "6000.00", 12);
        partialPaymentLoan.approve();
        partialPaymentLoan.disburse();
        when(loanRepository.findById(LoanId.of("LOAN-SVC-006"))).thenReturn(Optional.of(partialPaymentLoan));
        when(loanRepository.save(partialPaymentLoan)).thenReturn(partialPaymentLoan);

        LoanResponse partial = service.makePayment("LOAN-SVC-006", Money.aed(new BigDecimal("1000.00")));

        assertThat(partial.outstandingBalance()).isEqualByComparingTo("5000.00");
        verify(customerCreditService, never()).releaseCredit(
            eq(partialPaymentLoan.getCustomerId()),
            eq(partialPaymentLoan.getPrincipalAmount())
        );
    }

    @Test
    void findAndGetCustomerIdShouldReturnLoanData() {
        Loan loan = loan("LOAN-SVC-007", "9000.00", 12);
        when(loanRepository.findById(LoanId.of("LOAN-SVC-007"))).thenReturn(Optional.of(loan));

        LoanResponse found = service.findLoanById("LOAN-SVC-007");
        String customerId = service.getCustomerIdForLoan("LOAN-SVC-007");

        assertThat(found.loanId()).isEqualTo("LOAN-SVC-007");
        assertThat(customerId).isEqualTo(loan.getCustomerId().getValue());
    }

    @Test
    void methodsShouldThrowLoanNotFoundWhenMissing() {
        when(loanRepository.findById(any(LoanId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveLoan("MISSING"))
            .isInstanceOf(LoanNotFoundException.class);
        assertThatThrownBy(() -> service.rejectLoan("MISSING", "reason"))
            .isInstanceOf(LoanNotFoundException.class);
        assertThatThrownBy(() -> service.disburseLoan("MISSING"))
            .isInstanceOf(LoanNotFoundException.class);
        assertThatThrownBy(() -> service.makePayment("MISSING", Money.aed(new BigDecimal("1.00"))))
            .isInstanceOf(LoanNotFoundException.class);
        assertThatThrownBy(() -> service.findLoanById("MISSING"))
            .isInstanceOf(LoanNotFoundException.class);
        assertThatThrownBy(() -> service.cancelLoan("MISSING", "reason"))
            .isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void createLoanApplicationShouldPassExpectedArgumentsToCreditService() {
        CreateLoanRequest request = new CreateLoanRequest(
            "CUST-LOAN-008",
            new BigDecimal("30000.00"),
            "AED",
            new BigDecimal("8.0"),
            36
        );
        when(customerCreditService.hasAvailableCredit(any(CustomerId.class), any(Money.class))).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createLoanApplication(request);

        ArgumentCaptor<CustomerId> customerIdCaptor = ArgumentCaptor.forClass(CustomerId.class);
        ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);
        verify(customerCreditService).hasAvailableCredit(customerIdCaptor.capture(), moneyCaptor.capture());
        assertThat(customerIdCaptor.getValue().getValue()).isEqualTo("CUST-LOAN-008");
        assertThat(moneyCaptor.getValue().getAmount()).isEqualByComparingTo("30000.00");
    }

    private static Loan loan(String loanId, String amount, int termMonths) {
        return Loan.create(
            LoanId.of(loanId),
            CustomerId.of("CUST-" + loanId.substring(5)),
            Money.aed(new BigDecimal(amount)),
            InterestRate.of(new BigDecimal("6.0")),
            LoanTerm.ofMonths(termMonths)
        );
    }
}
