package com.loanmanagement.application.service;

import com.loanmanagement.application.dto.*;
import com.loanmanagement.application.usecase.*;
import com.loanmanagement.domain.event.DomainEvent;
import com.loanmanagement.loan.domain.aggregate.Loan;
import com.loanmanagement.domain.model.entity.*;
import com.loanmanagement.domain.model.value.*;
import com.loanmanagement.domain.service.CreditAssessmentService;
import com.loanmanagement.domain.model.value.InstallmentCount;
import com.loanmanagement.infrastructure.persistence.repository.EventStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.loanmanagement.domain.port.DomainEventPublisher;

@Service
@Transactional
public class LoanApplicationService implements CreateLoanUseCase, ListLoansUseCase, 
    ListInstallmentsUseCase, PayLoanUseCase {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final CreditAssessmentService creditAssessmentService;
    private final DomainEventPublisher eventPublisher;
    private final EventStoreRepository eventStore;

    public LoanApplicationService(
            CustomerRepository customerRepository,
            LoanRepository loanRepository,
            CreditAssessmentService creditAssessmentService,
            DomainEventPublisher eventPublisher,
            EventStoreRepository eventStore
    ) {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.creditAssessmentService = creditAssessmentService;
        this.eventPublisher = eventPublisher;
        this.eventStore = eventStore;
    }
    
    @Override
    public CreateLoanResponse execute(CreateLoanRequest request) {
        // Validate installment count
        InstallmentCount installmentCount = new InstallmentCount(request.numberOfInstallments());
        
        // Find customer
        Customer customer = customerRepository.findById(request.customerId())
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + request.customerId()));
        
        // Validate credit
        Money principalAmount = new Money(request.amount());
        creditAssessmentService.validateCreditEligibility(customer, principalAmount);
        
        // Create loan
        InterestRate interestRate = new InterestRate(request.interestRate());
        Long loanId = loanRepository.nextId();
        
        Loan loan = Loan.create(
            loanId,
            customer.getId(),
            principalAmount,
            interestRate,
            installmentCount
        );

        // Reserve credit
        customer.reserveCredit(principalAmount);

        // Save entities
        loan = loanRepository.save(loan);
        customerRepository.save(customer);

        // Publish events
        publishAndStoreEvents(loan);
        publishAndStoreEvents(customer);
        
        // Map to response
        List<InstallmentDto> installmentDtos = loan.getInstallments().stream()
            .map(this::mapToInstallmentDto)
            .collect(Collectors.toList());
        
        return new CreateLoanResponse(
            loan.getId(),
            loan.getLoanAmount().getValue(),
            installmentCount.getValue(),
            installmentDtos
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LoanDto> execute(Long customerId, Integer numberOfInstallments, Boolean isPaid) {
        List<Loan> loans = loanRepository.findByCustomerIdWithFilters(
            customerId, numberOfInstallments, isPaid
        );
        
        return loans.stream()
            .map(this::mapToLoanDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDto> execute(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
        
        return loan.getInstallments().stream()
            .map(this::mapToInstallmentDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public PayLoanResponse execute(PayLoanRequest request) {
        // Find loan
        Loan loan = loanRepository.findById(request.loanId())
            .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + request.loanId()));
        
        // Make payment
        Money paymentAmount = new Money(request.amount());
        PaymentResult result = loan.makePayment(paymentAmount, LocalDate.now());
        
        // Update loan
        loanRepository.save(loan);

        // If loan is fully paid, release customer credit
        if (result.isLoanFullyPaid()) {
            Customer customer = customerRepository.findById(loan.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

            customer.releaseCredit(loan.getPrincipalAmount());
            customerRepository.save(customer);

            // Publish customer events
            publishAndStoreEvents(customer);
        }

        // Publish loan events
        publishAndStoreEvents(loan);

        return new PayLoanResponse(
                result.getInstallmentsPaid(),
                result.getTotalAmountSpent().getValue(),
                result.isLoanFullyPaid()
        );
    }

    private void publishAndStoreEvents(Loan loan) {
        List<DomainEvent> events = loan.getAndClearEvents();
        events.forEach(event -> {
            eventStore.save(event);
            eventPublisher.publish(event);
        });
    }

    private void publishAndStoreEvents(Customer customer) {
        List<DomainEvent> events = customer.getAndClearEvents();
        events.forEach(event -> {
            eventStore.save(event);
            eventPublisher.publish(event);
        }
        );}
    private LoanDto mapToLoanDto(Loan loan) {
        return new LoanDto(
            loan.getId(),
            loan.getCustomerId(),
            loan.getLoanAmount().getValue(),
            loan.getInstallments().size(),
            loan.getCreateDate(),
            loan.isPaid()
        );
    }
    
    private InstallmentDto mapToInstallmentDto(LoanInstallment installment) {
        return new InstallmentDto(
            installment.getId(),
            installment.getAmount().getValue(),
            installment.getPaidAmount() != null ? installment.getPaidAmount().getValue() : null,
            installment.getDueDate(),
            installment.getPaymentDate(),
            installment.isPaid()
        );
    }
}