package com.bank.loanmanagement.microservices.loan;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

/**
 * Loan Origination Microservice with Isolated Database
 * Implements hexagonal architecture with SAGA orchestration
 */
@RestController
@RequestMapping("/api/v1/loans")
@ConditionalOnProperty(name = "microservices.loan.enabled", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackages = "com.bank.loanmanagement.microservices.loan.domain")
@EnableJpaRepositories(basePackages = "com.bank.loanmanagement.microservices.loan.infrastructure")
@Slf4j
public class LoanOriginationMicroservice {

    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private LoanInstallmentRepository installmentRepository;
    
    @Autowired
    private LoanDomainService loanDomainService;
    
    @Autowired
    private LoanCreationSagaOrchestrator sagaOrchestrator;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create Loan Application (Initiates SAGA)
     */
    @PostMapping
    @Transactional
    public ResponseEntity<LoanApplicationResponse> createLoanApplication(@Valid @RequestBody CreateLoanRequest request) {
        log.info("Creating loan application for customer: {} amount: {}", request.getCustomerId(), request.getAmount());
        
        // Validate business rules
        if (!loanDomainService.isValidInstallmentCount(request.getNumberOfInstallments())) {
            return ResponseEntity.badRequest().body(LoanApplicationResponse.builder()
                .successful(false)
                .error("Invalid installment count. Must be 6, 9, 12, or 24")
                .build());
        }
        
        if (!loanDomainService.isValidInterestRate(request.getInterestRate())) {
            return ResponseEntity.badRequest().body(LoanApplicationResponse.builder()
                .successful(false)
                .error("Invalid interest rate. Must be between 0.1 and 0.5")
                .build());
        }
        
        // Create loan application
        LoanApplication application = LoanApplication.builder()
            .customerId(request.getCustomerId())
            .requestedAmount(request.getAmount())
            .interestRate(request.getInterestRate())
            .numberOfInstallments(request.getNumberOfInstallments())
            .status(LoanApplicationStatus.PENDING)
            .applicationId(java.util.UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
            .build();
        
        // Start SAGA orchestration
        LoanApplicationSubmittedEvent event = LoanApplicationSubmittedEvent.builder()
            .applicationId(application.getApplicationId())
            .customerId(request.getCustomerId())
            .requestedAmount(request.getAmount())
            .interestRate(request.getInterestRate())
            .numberOfInstallments(request.getNumberOfInstallments())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("loan-saga-events", event);
        log.info("Started loan creation SAGA for application: {}", application.getApplicationId());
        
        return ResponseEntity.ok(LoanApplicationResponse.builder()
            .successful(true)
            .applicationId(application.getApplicationId())
            .status(LoanApplicationStatus.PENDING)
            .build());
    }

    /**
     * Get Loan Details
     */
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable Long loanId) {
        Optional<Loan> loan = loanRepository.findById(loanId);
        
        if (loan.isPresent()) {
            List<LoanInstallment> installments = installmentRepository.findByLoanIdOrderByDueDate(loanId);
            return ResponseEntity.ok(LoanResponse.fromDomain(loan.get(), installments));
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Get Customer Loans with Filtering
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoanResponse>> getCustomerLoans(
            @PathVariable Long customerId,
            @RequestParam(required = false) Integer numberOfInstallments,
            @RequestParam(required = false) Boolean isPaid) {
        
        List<Loan> loans;
        
        if (numberOfInstallments != null && isPaid != null) {
            loans = loanRepository.findByCustomerIdAndNumberOfInstallmentsAndIsPaid(customerId, numberOfInstallments, isPaid);
        } else if (numberOfInstallments != null) {
            loans = loanRepository.findByCustomerIdAndNumberOfInstallments(customerId, numberOfInstallments);
        } else if (isPaid != null) {
            loans = loanRepository.findByCustomerIdAndIsPaid(customerId, isPaid);
        } else {
            loans = loanRepository.findByCustomerId(customerId);
        }
        
        List<LoanResponse> responses = loans.stream()
            .map(loan -> {
                List<LoanInstallment> installments = installmentRepository.findByLoanIdOrderByDueDate(loan.getId());
                return LoanResponse.fromDomain(loan, installments);
            })
            .toList();
            
        return ResponseEntity.ok(responses);
    }

    /**
     * Get Loan Installments
     */
    @GetMapping("/{loanId}/installments")
    public ResponseEntity<List<InstallmentResponse>> getLoanInstallments(@PathVariable Long loanId) {
        List<LoanInstallment> installments = installmentRepository.findByLoanIdOrderByDueDate(loanId);
        
        List<InstallmentResponse> responses = installments.stream()
            .map(InstallmentResponse::fromDomain)
            .toList();
            
        return ResponseEntity.ok(responses);
    }

    /**
     * SAGA Event Handlers
     */
    @KafkaListener(topics = "credit-events", groupId = "loan-service")
    public void handleCreditEvents(Object event) {
        log.info("Received credit event: {}", event);
        // Handle credit reservation events from Customer service
        sagaOrchestrator.handleCreditEvent(event);
    }

    @KafkaListener(topics = "loan-saga-events", groupId = "loan-service")
    public void handleLoanSagaEvents(Object event) {
        log.info("Received loan SAGA event: {}", event);
        sagaOrchestrator.handleSagaEvent(event);
    }
    
    /**
     * Internal method to create loan (called by SAGA)
     */
    @Transactional
    public Loan createLoanInternal(CreateLoanCommand command) {
        log.info("Creating loan internally for customer: {}", command.getCustomerId());
        
        // Calculate total loan amount
        BigDecimal totalAmount = loanDomainService.calculateTotalAmount(
            command.getPrincipal(), 
            command.getInterestRate()
        );
        
        // Create loan entity
        Loan loan = Loan.builder()
            .customerId(command.getCustomerId())
            .loanAmount(command.getPrincipal())
            .totalAmount(totalAmount)
            .interestRate(command.getInterestRate())
            .numberOfInstallments(command.getNumberOfInstallments())
            .isPaid(false)
            .createdAt(LocalDateTime.now())
            .build();
            
        Loan savedLoan = loanRepository.save(loan);
        
        // Generate installment schedule
        List<LoanInstallment> installments = loanDomainService.generateInstallmentSchedule(
            savedLoan.getId(),
            totalAmount,
            command.getNumberOfInstallments()
        );
        
        installmentRepository.saveAll(installments);
        
        // Publish loan created event
        LoanCreatedEvent event = LoanCreatedEvent.builder()
            .loanId(savedLoan.getId())
            .customerId(command.getCustomerId())
            .amount(command.getPrincipal())
            .totalAmount(totalAmount)
            .numberOfInstallments(command.getNumberOfInstallments())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("loan-events", event);
        
        return savedLoan;
    }
}

/**
 * Loan Domain Entity
 */
@Entity
@Table(name = "loans", schema = "loan_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    private Long customerId;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal loanAmount;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalAmount;
    
    @DecimalMin(value = "0.1")
    @DecimalMax(value = "0.5")
    private BigDecimal interestRate;
    
    @Min(6)
    @Max(24)
    private Integer numberOfInstallments;
    
    private Boolean isPaid;
    
    private LocalDateTime createdAt;
    
    // Domain methods
    public boolean isFullyPaid() {
        return Boolean.TRUE.equals(isPaid);
    }
    
    public void markAsPaid() {
        this.isPaid = true;
    }
}

/**
 * Loan Installment Entity
 */
@Entity
@Table(name = "loan_installments", schema = "loan_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    private Long loanId;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    
    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal paidAmount;
    
    @NotNull
    private LocalDate dueDate;
    
    private LocalDate paymentDate;
    
    private Boolean isPaid;
    
    // Domain methods
    public BigDecimal getRemainingAmount() {
        return amount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
    
    public boolean isFullyPaid() {
        return Boolean.TRUE.equals(isPaid);
    }
    
    public void markAsPaid(LocalDate paymentDate) {
        this.isPaid = true;
        this.paidAmount = this.amount;
        this.paymentDate = paymentDate;
    }
}

/**
 * Loan Application Value Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanApplication {
    private String applicationId;
    private Long customerId;
    private BigDecimal requestedAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LoanApplicationStatus status;
    private LocalDateTime createdAt;
}

enum LoanApplicationStatus {
    PENDING, APPROVED, REJECTED, CANCELLED
}

/**
 * Loan Domain Service
 */
@org.springframework.stereotype.Service
class LoanDomainService {
    
    public boolean isValidInstallmentCount(Integer count) {
        return count != null && (count == 6 || count == 9 || count == 12 || count == 24);
    }
    
    public boolean isValidInterestRate(BigDecimal rate) {
        return rate != null && 
               rate.compareTo(new BigDecimal("0.1")) >= 0 && 
               rate.compareTo(new BigDecimal("0.5")) <= 0;
    }
    
    public BigDecimal calculateTotalAmount(BigDecimal principal, BigDecimal interestRate) {
        return principal.multiply(BigDecimal.ONE.add(interestRate))
                       .setScale(2, RoundingMode.HALF_UP);
    }
    
    public List<LoanInstallment> generateInstallmentSchedule(Long loanId, BigDecimal totalAmount, Integer numberOfInstallments) {
        List<LoanInstallment> installments = new ArrayList<>();
        BigDecimal installmentAmount = totalAmount.divide(new BigDecimal(numberOfInstallments), 2, RoundingMode.HALF_UP);
        
        LocalDate firstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        
        for (int i = 0; i < numberOfInstallments; i++) {
            LoanInstallment installment = LoanInstallment.builder()
                .loanId(loanId)
                .amount(installmentAmount)
                .paidAmount(BigDecimal.ZERO)
                .dueDate(firstDueDate.plusMonths(i))
                .isPaid(false)
                .build();
            installments.add(installment);
        }
        
        return installments;
    }
}

/**
 * SAGA Orchestrator for Loan Creation
 */
@org.springframework.stereotype.Service
class LoanCreationSagaOrchestrator {
    
    @Autowired
    private LoanOriginationMicroservice loanService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void handleCreditEvent(Object event) {
        // Handle credit reservation success/failure
        log.info("Processing credit event in SAGA: {}", event);
    }
    
    public void handleSagaEvent(Object event) {
        // Handle SAGA orchestration events
        log.info("Processing SAGA event: {}", event);
    }
    
    public void compensate(String sagaId, String reason) {
        // Implement compensation logic
        log.info("Compensating SAGA: {} reason: {}", sagaId, reason);
    }
}

/**
 * Repository Interfaces
 */
interface LoanRepository extends org.springframework.data.jpa.repository.JpaRepository<Loan, Long> {
    List<Loan> findByCustomerId(Long customerId);
    List<Loan> findByCustomerIdAndNumberOfInstallments(Long customerId, Integer numberOfInstallments);
    List<Loan> findByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);
    List<Loan> findByCustomerIdAndNumberOfInstallmentsAndIsPaid(Long customerId, Integer numberOfInstallments, Boolean isPaid);
}

interface LoanInstallmentRepository extends org.springframework.data.jpa.repository.JpaRepository<LoanInstallment, Long> {
    List<LoanInstallment> findByLoanIdOrderByDueDate(Long loanId);
    List<LoanInstallment> findByLoanIdAndIsPaidFalseOrderByDueDate(Long loanId);
}

// DTOs and Commands
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateLoanRequest {
    @NotNull
    private Long customerId;
    @DecimalMin("1.00")
    private BigDecimal amount;
    @DecimalMin("0.1")
    @DecimalMax("0.5")
    private BigDecimal interestRate;
    @NotNull
    @Min(6)
    @Max(24)
    private Integer numberOfInstallments;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanApplicationResponse {
    private boolean successful;
    private String applicationId;
    private LoanApplicationStatus status;
    private String error;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanResponse {
    private Long id;
    private Long customerId;
    private BigDecimal loanAmount;
    private BigDecimal totalAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private Boolean isPaid;
    private LocalDateTime createdAt;
    private List<InstallmentResponse> installments;
    
    public static LoanResponse fromDomain(Loan loan, List<LoanInstallment> installments) {
        return LoanResponse.builder()
            .id(loan.getId())
            .customerId(loan.getCustomerId())
            .loanAmount(loan.getLoanAmount())
            .totalAmount(loan.getTotalAmount())
            .interestRate(loan.getInterestRate())
            .numberOfInstallments(loan.getNumberOfInstallments())
            .isPaid(loan.getIsPaid())
            .createdAt(loan.getCreatedAt())
            .installments(installments.stream().map(InstallmentResponse::fromDomain).toList())
            .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class InstallmentResponse {
    private Long id;
    private Long loanId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private Boolean isPaid;
    
    public static InstallmentResponse fromDomain(LoanInstallment installment) {
        return InstallmentResponse.builder()
            .id(installment.getId())
            .loanId(installment.getLoanId())
            .amount(installment.getAmount())
            .paidAmount(installment.getPaidAmount())
            .remainingAmount(installment.getRemainingAmount())
            .dueDate(installment.getDueDate())
            .paymentDate(installment.getPaymentDate())
            .isPaid(installment.getIsPaid())
            .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateLoanCommand {
    private String sagaId;
    private Long customerId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
}

// Domain Events
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanApplicationSubmittedEvent {
    private String eventId;
    private String applicationId;
    private Long customerId;
    private BigDecimal requestedAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanCreatedEvent {
    private String eventId;
    private Long loanId;
    private Long customerId;
    private BigDecimal amount;
    private BigDecimal totalAmount;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class InstallmentScheduleGeneratedEvent {
    private String eventId;
    private Long loanId;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}