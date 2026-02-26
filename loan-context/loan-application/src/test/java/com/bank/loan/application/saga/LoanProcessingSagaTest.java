package com.bank.loan.application.saga;

import com.bank.loan.application.LoanManagementService;
import com.bank.loan.domain.LoanApprovedEvent;
import com.bank.loan.domain.LoanDisbursedEvent;
import com.bank.loan.domain.LoanId;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import com.bank.shared.kernel.event.CustomerCreatedEvent;
import com.bank.shared.kernel.event.CustomerCreditReservedEvent;
import com.bank.shared.kernel.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LoanProcessingSagaTest {

    @Mock
    private LoanManagementService loanService;

    @Test
    void handlersShouldProcessEventsWithoutCallingLoanServiceDirectly() {
        LoanProcessingSaga saga = new LoanProcessingSaga(loanService);
        CustomerId customerId = CustomerId.of("CUST-SAGA-001");
        LoanId loanId = LoanId.of("LOAN-SAGA-001");

        saga.handleCustomerCreated(new CustomerCreatedEvent(customerId, "Ali Copur"));
        saga.handleLoanApproved(new LoanApprovedEvent(loanId, customerId, Money.aed(new BigDecimal("10000.00"))));
        saga.handleCreditReserved(new CustomerCreditReservedEvent(customerId, Money.aed(new BigDecimal("10000.00"))));
        saga.handleLoanDisbursed(
            new LoanDisbursedEvent(loanId, customerId, Money.aed(new BigDecimal("10000.00")), LocalDate.now())
        );
        saga.handlePaymentCompleted(
            new PaymentCompletedEvent("PAY-SAGA-001", customerId, Money.aed(new BigDecimal("500.00")))
        );

        verifyNoInteractions(loanService);
    }
}
