package com.bank.loanmanagement.microservices.customer;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Customer Management Microservice with Isolated Database
 * Implements hexagonal architecture with domain-driven design
 */
@RestController
@RequestMapping("/api/v1/customers")
@ConditionalOnProperty(name = "microservices.customer.enabled", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackages = "com.bank.loanmanagement.microservices.customer.domain")
@EnableJpaRepositories(basePackages = "com.bank.loanmanagement.microservices.customer.infrastructure")
@Slf4j
public class CustomerMicroservice {

    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CustomerDomainService customerDomainService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create Customer with Credit Limit
     */
    @PostMapping
    @Transactional
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        log.info("Creating customer: {}", request.getEmail());
        
        // Domain validation
        Customer customer = Customer.builder()
            .name(request.getName())
            .surname(request.getSurname())
            .email(request.getEmail())
            .creditLimit(request.getCreditLimit())
            .usedCreditLimit(BigDecimal.ZERO)
            .accountStatus(AccountStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
            
        Customer savedCustomer = customerRepository.save(customer);
        
        // Publish domain event
        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
            .customerId(savedCustomer.getId())
            .email(savedCustomer.getEmail())
            .creditLimit(savedCustomer.getCreditLimit())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("customer-events", event);
        log.info("Published CustomerCreatedEvent for customer: {}", savedCustomer.getId());
        
        return ResponseEntity.ok(CustomerResponse.fromDomain(savedCustomer));
    }

    /**
     * Get Customer Profile
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long customerId) {
        Optional<Customer> customer = customerRepository.findById(customerId);
        
        if (customer.isPresent()) {
            return ResponseEntity.ok(CustomerResponse.fromDomain(customer.get()));
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Reserve Credit for Loan (SAGA Pattern)
     */
    @PostMapping("/{customerId}/credit/reserve")
    @Transactional
    public ResponseEntity<CreditReservationResponse> reserveCredit(
            @PathVariable Long customerId,
            @Valid @RequestBody CreditReservationRequest request) {
        
        log.info("Reserving credit for customer: {} amount: {}", customerId, request.getAmount());
        
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Customer customer = customerOpt.get();
        
        // Domain business logic
        boolean reservationSuccessful = customerDomainService.reserveCredit(customer, request.getAmount());
        
        if (reservationSuccessful) {
            customerRepository.save(customer);
            
            // Publish success event
            CreditReservedEvent event = CreditReservedEvent.builder()
                .customerId(customerId)
                .amount(request.getAmount())
                .reservationId(request.getReservationId())
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
                
            kafkaTemplate.send("credit-events", event);
            
            return ResponseEntity.ok(CreditReservationResponse.builder()
                .successful(true)
                .reservationId(request.getReservationId())
                .availableCredit(customer.getAvailableCredit())
                .build());
        } else {
            // Publish failure event
            CreditReservationFailedEvent event = CreditReservationFailedEvent.builder()
                .customerId(customerId)
                .amount(request.getAmount())
                .reservationId(request.getReservationId())
                .reason("Insufficient credit limit")
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();
                
            kafkaTemplate.send("credit-events", event);
            
            return ResponseEntity.badRequest().body(CreditReservationResponse.builder()
                .successful(false)
                .reason("Insufficient credit limit")
                .availableCredit(customer.getAvailableCredit())
                .build());
        }
    }

    /**
     * Release Credit (SAGA Compensation)
     */
    @PostMapping("/{customerId}/credit/release")
    @Transactional
    public ResponseEntity<CreditReleaseResponse> releaseCredit(
            @PathVariable Long customerId,
            @Valid @RequestBody CreditReleaseRequest request) {
        
        log.info("Releasing credit for customer: {} amount: {}", customerId, request.getAmount());
        
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Customer customer = customerOpt.get();
        customerDomainService.releaseCredit(customer, request.getAmount());
        customerRepository.save(customer);
        
        // Publish event
        CreditReleasedEvent event = CreditReleasedEvent.builder()
            .customerId(customerId)
            .amount(request.getAmount())
            .reservationId(request.getReservationId())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("credit-events", event);
        
        return ResponseEntity.ok(CreditReleaseResponse.builder()
            .successful(true)
            .availableCredit(customer.getAvailableCredit())
            .build());
    }

    /**
     * SAGA Event Listeners for Distributed Transactions
     */
    @KafkaListener(topics = "loan-events", groupId = "customer-service")
    public void handleLoanEvents(Object event) {
        log.info("Received loan event: {}", event);
        // Handle loan-related events that affect customer credit
    }

    @KafkaListener(topics = "payment-events", groupId = "customer-service")
    public void handlePaymentEvents(Object event) {
        log.info("Received payment event: {}", event);
        // Handle payment events that release customer credit
    }
}

/**
 * Customer Domain Entity with Hexagonal Architecture
 */
@Entity
@Table(name = "customers", schema = "customer_db")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(min = 2, max = 50)
    private String name;
    
    @NotBlank
    @Size(min = 2, max = 50)
    private String surname;
    
    @Email
    @Column(unique = true)
    private String email;
    
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;
    
    @DecimalMin(value = "0.0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal usedCreditLimit;
    
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Domain methods
    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(usedCreditLimit);
    }
    
    public boolean canReserveCredit(BigDecimal amount) {
        return getAvailableCredit().compareTo(amount) >= 0;
    }
    
    public void reserveCredit(BigDecimal amount) {
        if (!canReserveCredit(amount)) {
            throw new IllegalStateException("Insufficient credit limit");
        }
        this.usedCreditLimit = this.usedCreditLimit.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void releaseCredit(BigDecimal amount) {
        this.usedCreditLimit = this.usedCreditLimit.subtract(amount);
        if (this.usedCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
            this.usedCreditLimit = BigDecimal.ZERO;
        }
        this.updatedAt = LocalDateTime.now();
    }
}

enum AccountStatus {
    ACTIVE, SUSPENDED, CLOSED
}

/**
 * Customer Domain Service
 */
@org.springframework.stereotype.Service
class CustomerDomainService {
    
    public boolean reserveCredit(Customer customer, BigDecimal amount) {
        if (customer.canReserveCredit(amount)) {
            customer.reserveCredit(amount);
            return true;
        }
        return false;
    }
    
    public void releaseCredit(Customer customer, BigDecimal amount) {
        customer.releaseCredit(amount);
    }
}

/**
 * Customer Repository Interface
 */
interface CustomerRepository extends org.springframework.data.jpa.repository.JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    List<Customer> findByAccountStatus(AccountStatus status);
}

// DTOs and Events
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateCustomerRequest {
    @NotBlank
    private String name;
    @NotBlank 
    private String surname;
    @Email
    private String email;
    @DecimalMin("1000.00")
    private BigDecimal creditLimit;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CustomerResponse {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private BigDecimal creditLimit;
    private BigDecimal usedCreditLimit;
    private BigDecimal availableCredit;
    private AccountStatus accountStatus;
    private LocalDateTime createdAt;
    
    public static CustomerResponse fromDomain(Customer customer) {
        return CustomerResponse.builder()
            .id(customer.getId())
            .name(customer.getName())
            .surname(customer.getSurname())
            .email(customer.getEmail())
            .creditLimit(customer.getCreditLimit())
            .usedCreditLimit(customer.getUsedCreditLimit())
            .availableCredit(customer.getAvailableCredit())
            .accountStatus(customer.getAccountStatus())
            .createdAt(customer.getCreatedAt())
            .build();
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReservationRequest {
    @NotNull
    private String reservationId;
    @DecimalMin("0.01")
    private BigDecimal amount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReservationResponse {
    private boolean successful;
    private String reservationId;
    private String reason;
    private BigDecimal availableCredit;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReleaseRequest {
    @NotNull
    private String reservationId;
    @DecimalMin("0.01")
    private BigDecimal amount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReleaseResponse {
    private boolean successful;
    private BigDecimal availableCredit;
}

// Domain Events for SAGA Pattern
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CustomerCreatedEvent {
    private String eventId;
    private Long customerId;
    private String email;
    private BigDecimal creditLimit;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReservedEvent {
    private String eventId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReservationFailedEvent {
    private String eventId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private String reason;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReleasedEvent {
    private String eventId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private LocalDateTime timestamp;
}