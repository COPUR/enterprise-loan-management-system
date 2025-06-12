package com.bank.loanmanagement.microservices.payment;

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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Payment Processing Microservice with Isolated Database
 * Implements hexagonal architecture with SAGA pattern for distributed transactions
 */
@RestController
@RequestMapping("/api/v1/payments")
@ConditionalOnProperty(name = "microservices.payment.enabled", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackages = "com.bank.loanmanagement.microservices.payment.domain")
@EnableJpaRepositories(basePackages = "com.bank.loanmanagement.microservices.payment.infrastructure")
@Slf4j
public class PaymentProcessingMicroservice {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentInstallmentRepository paymentInstallmentRepository;
    
    @Autowired
    private PaymentDomainService paymentDomainService;
    
    @Autowired
    private PaymentProcessingSagaOrchestrator sagaOrchestrator;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Process Payment (Initiates SAGA)
     */
    @PostMapping("/{loanId}")
    @Transactional
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long loanId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        
        log.info("Processing payment for loan: {} amount: {}", loanId, request.getAmount());
        
        // Validate payment amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(PaymentResponse.builder()
                .successful(false)
                .error("Payment amount must be greater than zero")
                .build());
        }
        
        // Create payment record
        Payment payment = Payment.builder()
            .loanId(loanId)
            .customerId(request.getCustomerId())
            .amount(request.getAmount())
            .paymentDate(LocalDate.now())
            .status(PaymentStatus.INITIATED)
            .paymentId(java.util.UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
            .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Start Payment Processing SAGA
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
            .paymentId(savedPayment.getPaymentId())
            .loanId(loanId)
            .customerId(request.getCustomerId())
            .amount(request.getAmount())
            .paymentDate(LocalDate.now())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("payment-saga-events", event);
        log.info("Started payment processing SAGA for payment: {}", savedPayment.getPaymentId());
        
        return ResponseEntity.ok(PaymentResponse.builder()
            .successful(true)
            .paymentId(savedPayment.getPaymentId())
            .status(PaymentStatus.INITIATED)
            .message("Payment processing initiated")
            .build());
    }

    /**
     * Get Payment Details
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailsResponse> getPayment(@PathVariable String paymentId) {
        Optional<Payment> payment = paymentRepository.findByPaymentId(paymentId);
        
        if (payment.isPresent()) {
            List<PaymentInstallment> installments = paymentInstallmentRepository.findByPaymentId(paymentId);
            return ResponseEntity.ok(PaymentDetailsResponse.fromDomain(payment.get(), installments));
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Get Customer Payment History
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<PaymentDetailsResponse>> getCustomerPayments(@PathVariable Long customerId) {
        List<Payment> payments = paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        
        List<PaymentDetailsResponse> responses = payments.stream()
            .map(payment -> {
                List<PaymentInstallment> installments = paymentInstallmentRepository.findByPaymentId(payment.getPaymentId());
                return PaymentDetailsResponse.fromDomain(payment, installments);
            })
            .toList();
            
        return ResponseEntity.ok(responses);
    }

    /**
     * Get Loan Payment History
     */
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<PaymentDetailsResponse>> getLoanPayments(@PathVariable Long loanId) {
        List<Payment> payments = paymentRepository.findByLoanIdOrderByCreatedAtDesc(loanId);
        
        List<PaymentDetailsResponse> responses = payments.stream()
            .map(payment -> {
                List<PaymentInstallment> installments = paymentInstallmentRepository.findByPaymentId(payment.getPaymentId());
                return PaymentDetailsResponse.fromDomain(payment, installments);
            })
            .toList();
            
        return ResponseEntity.ok(responses);
    }

    /**
     * Internal Payment Processing (Called by SAGA)
     */
    @Transactional
    public PaymentProcessingResult processPaymentInternal(ProcessPaymentCommand command) {
        log.info("Processing payment internally: {}", command.getPaymentId());
        
        // Calculate payment distribution across installments
        PaymentCalculationResult calculation = paymentDomainService.calculatePaymentDistribution(
            command.getLoanId(),
            command.getAmount(),
            command.getPaymentDate(),
            command.getInstallments()
        );
        
        if (!calculation.isSuccessful()) {
            return PaymentProcessingResult.builder()
                .successful(false)
                .error(calculation.getError())
                .build();
        }
        
        // Process each installment payment
        BigDecimal totalPaid = BigDecimal.ZERO;
        int installmentsPaid = 0;
        
        for (InstallmentPayment installmentPayment : calculation.getInstallmentPayments()) {
            PaymentInstallment paymentInstallment = PaymentInstallment.builder()
                .paymentId(command.getPaymentId())
                .installmentId(installmentPayment.getInstallmentId())
                .originalAmount(installmentPayment.getOriginalAmount())
                .paidAmount(installmentPayment.getPaidAmount())
                .discountAmount(installmentPayment.getDiscountAmount())
                .penaltyAmount(installmentPayment.getPenaltyAmount())
                .effectiveAmount(installmentPayment.getEffectiveAmount())
                .paymentDate(command.getPaymentDate())
                .build();
                
            paymentInstallmentRepository.save(paymentInstallment);
            
            totalPaid = totalPaid.add(installmentPayment.getEffectiveAmount());
            installmentsPaid++;
        }
        
        // Update payment status
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(command.getPaymentId());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTotalPaid(totalPaid);
            payment.setInstallmentsPaid(installmentsPaid);
            paymentRepository.save(payment);
        }
        
        // Publish payment completed event
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
            .paymentId(command.getPaymentId())
            .loanId(command.getLoanId())
            .customerId(command.getCustomerId())
            .totalPaid(totalPaid)
            .installmentsPaid(installmentsPaid)
            .isLoanFullyPaid(calculation.isLoanFullyPaid())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("payment-events", event);
        
        return PaymentProcessingResult.builder()
            .successful(true)
            .totalPaid(totalPaid)
            .installmentsPaid(installmentsPaid)
            .isLoanFullyPaid(calculation.isLoanFullyPaid())
            .build();
    }

    /**
     * SAGA Event Handlers
     */
    @KafkaListener(topics = "loan-events", groupId = "payment-service")
    public void handleLoanEvents(Object event) {
        log.info("Received loan event: {}", event);
        sagaOrchestrator.handleLoanEvent(event);
    }

    @KafkaListener(topics = "payment-saga-events", groupId = "payment-service")
    public void handlePaymentSagaEvents(Object event) {
        log.info("Received payment SAGA event: {}", event);
        sagaOrchestrator.handleSagaEvent(event);
    }
}

/**
 * Payment Domain Entity
 */
@Entity
@Table(name = "payments", schema = "payment_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String paymentId;
    
    @NotNull
    private Long loanId;
    
    @NotNull
    private Long customerId;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    
    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal totalPaid;
    
    private Integer installmentsPaid;
    
    @NotNull
    private LocalDate paymentDate;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

/**
 * Payment Installment Entity
 */
@Entity
@Table(name = "payment_installments", schema = "payment_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    private String paymentId;
    
    @NotNull
    private Long installmentId;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal originalAmount;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal paidAmount;
    
    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal discountAmount;
    
    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal penaltyAmount;
    
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal effectiveAmount;
    
    @NotNull
    private LocalDate paymentDate;
}

enum PaymentStatus {
    INITIATED, VALIDATED, PROCESSING, COMPLETED, FAILED, CANCELLED
}

/**
 * Payment Domain Service
 */
@org.springframework.stereotype.Service
class PaymentDomainService {
    
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.001");
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.001");
    
    public PaymentCalculationResult calculatePaymentDistribution(
            Long loanId, 
            BigDecimal paymentAmount, 
            LocalDate paymentDate,
            List<LoanInstallmentInfo> installments) {
        
        // Sort installments by due date (pay earliest first)
        installments.sort((i1, i2) -> i1.getDueDate().compareTo(i2.getDueDate()));
        
        // Validate advance payment restriction (max 3 months)
        LocalDate maxAdvanceDate = paymentDate.plusMonths(3);
        List<LoanInstallmentInfo> payableInstallments = installments.stream()
            .filter(i -> !i.getIsPaid())
            .filter(i -> i.getDueDate().isBefore(maxAdvanceDate) || i.getDueDate().isEqual(maxAdvanceDate))
            .toList();
        
        if (payableInstallments.isEmpty()) {
            return PaymentCalculationResult.builder()
                .successful(false)
                .error("No installments available for payment within advance payment limit")
                .build();
        }
        
        // Calculate payment distribution
        BigDecimal remainingAmount = paymentAmount;
        List<InstallmentPayment> installmentPayments = new ArrayList<>();
        
        for (LoanInstallmentInfo installment : payableInstallments) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            BigDecimal installmentAmount = installment.getAmount().subtract(
                installment.getPaidAmount() != null ? installment.getPaidAmount() : BigDecimal.ZERO
            );
            
            if (remainingAmount.compareTo(installmentAmount) >= 0) {
                // Pay full installment
                BigDecimal discountAmount = calculateDiscount(installmentAmount, installment.getDueDate(), paymentDate);
                BigDecimal penaltyAmount = calculatePenalty(installmentAmount, installment.getDueDate(), paymentDate);
                BigDecimal effectiveAmount = installmentAmount.add(penaltyAmount).subtract(discountAmount);
                
                InstallmentPayment payment = InstallmentPayment.builder()
                    .installmentId(installment.getId())
                    .originalAmount(installmentAmount)
                    .paidAmount(installmentAmount)
                    .discountAmount(discountAmount)
                    .penaltyAmount(penaltyAmount)
                    .effectiveAmount(effectiveAmount)
                    .build();
                    
                installmentPayments.add(payment);
                remainingAmount = remainingAmount.subtract(effectiveAmount);
            }
        }
        
        // Check if all remaining installments are paid
        boolean isLoanFullyPaid = installments.stream()
            .filter(i -> !i.getIsPaid())
            .allMatch(i -> installmentPayments.stream()
                .anyMatch(p -> p.getInstallmentId().equals(i.getId())));
        
        return PaymentCalculationResult.builder()
            .successful(true)
            .installmentPayments(installmentPayments)
            .isLoanFullyPaid(isLoanFullyPaid)
            .build();
    }
    
    private BigDecimal calculateDiscount(BigDecimal amount, LocalDate dueDate, LocalDate paymentDate) {
        if (paymentDate.isBefore(dueDate)) {
            long daysBefore = ChronoUnit.DAYS.between(paymentDate, dueDate);
            return amount.multiply(DISCOUNT_RATE).multiply(new BigDecimal(daysBefore))
                        .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculatePenalty(BigDecimal amount, LocalDate dueDate, LocalDate paymentDate) {
        if (paymentDate.isAfter(dueDate)) {
            long daysAfter = ChronoUnit.DAYS.between(dueDate, paymentDate);
            return amount.multiply(PENALTY_RATE).multiply(new BigDecimal(daysAfter))
                        .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}

/**
 * Payment Processing SAGA Orchestrator
 */
@org.springframework.stereotype.Service
class PaymentProcessingSagaOrchestrator {
    
    @Autowired
    private PaymentProcessingMicroservice paymentService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void handleLoanEvent(Object event) {
        log.info("Processing loan event in payment SAGA: {}", event);
    }
    
    public void handleSagaEvent(Object event) {
        log.info("Processing payment SAGA event: {}", event);
    }
    
    public void compensate(String sagaId, String reason) {
        log.info("Compensating payment SAGA: {} reason: {}", sagaId, reason);
    }
}

/**
 * Repository Interfaces
 */
interface PaymentRepository extends org.springframework.data.jpa.repository.JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Payment> findByLoanIdOrderByCreatedAtDesc(Long loanId);
}

interface PaymentInstallmentRepository extends org.springframework.data.jpa.repository.JpaRepository<PaymentInstallment, Long> {
    List<PaymentInstallment> findByPaymentId(String paymentId);
}

// DTOs and Value Objects
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ProcessPaymentRequest {
    @NotNull
    private Long customerId;
    @DecimalMin("0.01")
    private BigDecimal amount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentResponse {
    private boolean successful;
    private String paymentId;
    private PaymentStatus status;
    private String message;
    private String error;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentDetailsResponse {
    private String paymentId;
    private Long loanId;
    private Long customerId;
    private BigDecimal amount;
    private BigDecimal totalPaid;
    private Integer installmentsPaid;
    private LocalDate paymentDate;
    private PaymentStatus status;
    private List<PaymentInstallmentResponse> installments;
    
    public static PaymentDetailsResponse fromDomain(Payment payment, List<PaymentInstallment> installments) {
        return PaymentDetailsResponse.builder()
            .paymentId(payment.getPaymentId())
            .loanId(payment.getLoanId())
            .customerId(payment.getCustomerId())
            .amount(payment.getAmount())
            .totalPaid(payment.getTotalPaid())
            .installmentsPaid(payment.getInstallmentsPaid())
            .paymentDate(payment.getPaymentDate())
            .status(payment.getStatus())
            .installments(installments.stream().map(PaymentInstallmentResponse::fromDomain).toList())
            .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentInstallmentResponse {
    private Long installmentId;
    private BigDecimal originalAmount;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal effectiveAmount;
    private LocalDate paymentDate;
    
    public static PaymentInstallmentResponse fromDomain(PaymentInstallment installment) {
        return PaymentInstallmentResponse.builder()
            .installmentId(installment.getInstallmentId())
            .originalAmount(installment.getOriginalAmount())
            .paidAmount(installment.getPaidAmount())
            .discountAmount(installment.getDiscountAmount())
            .penaltyAmount(installment.getPenaltyAmount())
            .effectiveAmount(installment.getEffectiveAmount())
            .paymentDate(installment.getPaymentDate())
            .build();
    }
}

// Commands and Results
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ProcessPaymentCommand {
    private String paymentId;
    private Long loanId;
    private Long customerId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private List<LoanInstallmentInfo> installments;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentProcessingResult {
    private boolean successful;
    private String error;
    private BigDecimal totalPaid;
    private Integer installmentsPaid;
    private boolean isLoanFullyPaid;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentCalculationResult {
    private boolean successful;
    private String error;
    private List<InstallmentPayment> installmentPayments;
    private boolean isLoanFullyPaid;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class InstallmentPayment {
    private Long installmentId;
    private BigDecimal originalAmount;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal effectiveAmount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanInstallmentInfo {
    private Long id;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private Boolean isPaid;
}

// Domain Events
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentInitiatedEvent {
    private String eventId;
    private String paymentId;
    private Long loanId;
    private Long customerId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentCompletedEvent {
    private String eventId;
    private String paymentId;
    private Long loanId;
    private Long customerId;
    private BigDecimal totalPaid;
    private Integer installmentsPaid;
    private boolean isLoanFullyPaid;
    private LocalDateTime timestamp;
}