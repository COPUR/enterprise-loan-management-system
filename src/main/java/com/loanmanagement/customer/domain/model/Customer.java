package com.loanmanagement.customer.domain.model;

import com.loanmanagement.customer.application.port.in.CreateCustomerUseCase;
import com.loanmanagement.customer.domain.event.CreditReleasedEvent;
import com.loanmanagement.customer.domain.event.CreditReservedEvent;
import com.loanmanagement.shared.domain.model.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "phone", nullable = false)
    private String phone;
    
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    
    @Column(name = "monthly_income", nullable = false)
    private String monthlyIncome;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Column(name = "credit_limit")
    private String creditLimit;
    
    @Column(name = "credit_limit_currency")
    private String creditLimitCurrency;
    
    @Column(name = "available_credit")
    private String availableCredit;
    
    @Column(name = "available_credit_currency")
    private String availableCreditCurrency;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "occupation")
    private String occupation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    private List<Object> domainEvents = new ArrayList<>();
    
    protected Customer() {
    }
    
    private Customer(Builder builder) {
        this.id = builder.id;
        this.firstName = Objects.requireNonNull(builder.firstName, "First name is required");
        this.lastName = Objects.requireNonNull(builder.lastName, "Last name is required");
        this.email = Objects.requireNonNull(builder.email, "Email is required");
        this.phone = Objects.requireNonNull(builder.phone, "Phone is required");
        this.dateOfBirth = Objects.requireNonNull(builder.dateOfBirth, "Date of birth is required");
        
        if (builder.monthlyIncome != null) {
            this.monthlyIncome = builder.monthlyIncome.getAmount().toString();
            this.currency = builder.monthlyIncome.getCurrency();
        }
        
        this.creditScore = builder.creditScore;
        this.address = builder.address;
        this.occupation = builder.occupation;
        this.status = builder.status != null ? builder.status : CustomerStatus.ACTIVE;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : LocalDateTime.now();
        
        if (builder.creditLimit != null) {
            this.creditLimit = builder.creditLimit.getAmount().toString();
            this.creditLimitCurrency = builder.creditLimit.getCurrency();
        }
        
        if (builder.availableCredit != null) {
            this.availableCredit = builder.availableCredit.getAmount().toString();
            this.availableCreditCurrency = builder.availableCredit.getCurrency();
        }
        
        validateCustomer();
    }
    
    public Customer(String firstName, String lastName, String email, String phone, 
                   LocalDate dateOfBirth, Money monthlyIncome) {
        this.firstName = Objects.requireNonNull(firstName, "First name cannot be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null");
        this.monthlyIncome = monthlyIncome.getAmount().toString();
        this.currency = monthlyIncome.getCurrency();
        this.status = CustomerStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        validateAge();
        validateEmail();
    }
    
    // Static factory method for creating from command
    public static Customer fromCreateCommand(CreateCustomerUseCase.CreateCustomerCommand command) {
        validateCreateCommand(command);
        
        Money calculatedCreditLimit = calculateInitialCreditLimit(command.monthlyIncome(), command.creditScore());
        
        return Customer.builder()
                .firstName(command.firstName())
                .lastName(command.lastName())
                .email(command.email())
                .phone(command.phone())
                .dateOfBirth(command.dateOfBirth())
                .monthlyIncome(command.monthlyIncome())
                .creditScore(command.creditScore())
                .address(command.address())
                .occupation(command.occupation())
                .creditLimit(calculatedCreditLimit)
                .availableCredit(calculatedCreditLimit)
                .status(CustomerStatus.ACTIVE)
                .build();
    }
    
    private static void validateCreateCommand(CreateCustomerUseCase.CreateCustomerCommand command) {
        if (command.firstName() == null || command.firstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (command.email() == null || !command.email().contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (command.creditScore() != null && (command.creditScore() < 300 || command.creditScore() > 850)) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
        if (command.dateOfBirth() != null) {
            int age = java.time.Period.between(command.dateOfBirth(), LocalDate.now()).getYears();
            if (age < 18) {
                throw new IllegalArgumentException("Customer must be at least 18 years old");
            }
        }
        if (command.monthlyIncome() != null && command.monthlyIncome().getAmount().compareTo(new BigDecimal("1000")) < 0) {
            throw new IllegalArgumentException("Monthly income must be at least $1000");
        }
    }
    
    private static Money calculateInitialCreditLimit(Money monthlyIncome, Integer creditScore) {
        if (monthlyIncome == null || creditScore == null) {
            return Money.zero(monthlyIncome != null ? monthlyIncome.getCurrency() : "USD");
        }
        
        BigDecimal multiplier;
        if (creditScore >= 750) {
            multiplier = new BigDecimal("2.0"); // Excellent credit: 2x monthly income
        } else if (creditScore >= 650) {
            multiplier = new BigDecimal("1.0"); // Fair credit: 1x monthly income
        } else {
            multiplier = new BigDecimal("0.5"); // Poor credit: 0.5x monthly income
        }
        
        return monthlyIncome.multiply(multiplier);
    }
    
    // Credit management methods
    public void reserveCredit(Money amount) {
        validateCreditOperation(amount, "Credit reservation amount must be positive");
        validateCustomerActive("Cannot reserve credit for inactive customer");
        validateCreditReservation(amount);
        
        Money currentAvailable = getAvailableCredit();
        Money newAvailable = currentAvailable.subtract(amount);
        
        this.availableCredit = newAvailable.getAmount().toString();
        this.availableCreditCurrency = newAvailable.getCurrency();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CreditReservedEvent(this.id, amount));
    }
    
    public void releaseCredit(Money amount) {
        validateCreditOperation(amount, "Credit release amount must be positive");
        
        Money currentAvailable = getAvailableCredit();
        Money newAvailable = currentAvailable.add(amount);
        
        this.availableCredit = newAvailable.getAmount().toString();
        this.availableCreditCurrency = newAvailable.getCurrency();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new CreditReleasedEvent(this.id, amount));
    }
    
    private void validateCreditOperation(Money amount, String message) {
        if (amount == null || amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
    
    private void validateCustomerActive(String message) {
        if (!isActive()) {
            throw new IllegalStateException(message);
        }
    }
    
    private void validateCreditReservation(Money amount) {
        Money available = getAvailableCredit();
        if (amount.isGreaterThan(available)) {
            throw new InsufficientCreditException("Insufficient credit available");
        }
    }
    
    public BigDecimal getCreditUtilizationRatio() {
        Money creditLimit = getCreditLimit();
        Money availableCredit = getAvailableCredit();
        
        if (creditLimit.isZero()) {
            return BigDecimal.ZERO;
        }
        
        Money usedCredit = creditLimit.subtract(availableCredit);
        return usedCredit.getAmount().divide(creditLimit.getAmount(), 2, RoundingMode.HALF_UP);
    }
    
    public boolean isEligibleForCreditIncrease() {
        if (creditScore == null || creditScore < 650) {
            return false;
        }
        
        BigDecimal utilization = getCreditUtilizationRatio();
        return utilization.compareTo(new BigDecimal("0.70")) < 0; // Less than 70% utilization
    }
    
    public String calculateRiskLevel() {
        if (creditScore == null || monthlyIncome == null) {
            return "UNKNOWN";
        }
        
        Money income = getMonthlyIncome();
        if (creditScore >= 750 && income.getAmount().compareTo(new BigDecimal("5000")) >= 0) {
            return "LOW";
        } else if (creditScore >= 650 && income.getAmount().compareTo(new BigDecimal("3000")) >= 0) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
    
    public Customer updateContactInfo(String phoneNumber, String address, String occupation) {
        return this.toBuilder()
                .phone(phoneNumber)
                .address(address)
                .occupation(occupation)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    public void updateContactInfo(String email, String phone) {
        if (email != null && !email.equals(this.email)) {
            validateEmail(email);
            this.email = email;
        }
        if (phone != null && !phone.equals(this.phone)) {
            this.phone = phone;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateIncome(Money newIncome) {
        Objects.requireNonNull(newIncome, "Income cannot be null");
        this.monthlyIncome = newIncome.getAmount().toString();
        this.currency = newIncome.getCurrency();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void reactivate() {
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isEligibleForLoan(Money requestedAmount) {
        if (!isActive()) {
            return false;
        }
        
        Money income = getMonthlyIncome();
        Money maxLoanAmount = income.multiply(java.math.BigDecimal.valueOf(36)); // 36 months income
        
        return requestedAmount.isLessThan(maxLoanAmount) || requestedAmount.equals(maxLoanAmount);
    }
    
    // Domain events management
    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    private void addDomainEvent(Object event) {
        domainEvents.add(event);
    }
    
    // Validation methods
    private void validateCustomer() {
        if (dateOfBirth != null) {
            validateAge();
        }
        if (email != null) {
            validateEmail();
        }
    }
    
    private void validateAge() {
        int age = getAge();
        if (age < 18) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
        if (age > 80) {
            throw new IllegalArgumentException("Customer must be under 80 years old");
        }
    }
    
    private void validateEmail() {
        validateEmail(this.email);
    }
    
    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    // Getters
    public Money getMonthlyIncome() {
        return Money.of(new java.math.BigDecimal(monthlyIncome), currency);
    }
    
    public Money getCreditLimit() {
        if (creditLimit == null || creditLimitCurrency == null) {
            return Money.zero(currency != null ? currency : "USD");
        }
        return Money.of(new BigDecimal(creditLimit), creditLimitCurrency);
    }
    
    public Money getAvailableCredit() {
        if (availableCredit == null || availableCreditCurrency == null) {
            return Money.zero(currency != null ? currency : "USD");
        }
        return Money.of(new BigDecimal(availableCredit), availableCreditCurrency);
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public boolean isActive() {
        return CustomerStatus.ACTIVE.equals(status);
    }
    
    public int getAge() {
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
    
    // Standard getters
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPhoneNumber() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Integer getCreditScore() { return creditScore; }
    public String getAddress() { return address; }
    public String getOccupation() { return occupation; }
    public CustomerStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .email(this.email)
                .phone(this.phone)
                .dateOfBirth(this.dateOfBirth)
                .monthlyIncome(getMonthlyIncome())
                .creditScore(this.creditScore)
                .address(this.address)
                .occupation(this.occupation)
                .creditLimit(getCreditLimit())
                .availableCredit(getAvailableCredit())
                .status(this.status)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt);
    }
    
    public static class Builder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private LocalDate dateOfBirth;
        private Money monthlyIncome;
        private Integer creditScore;
        private String address;
        private String occupation;
        private Money creditLimit;
        private Money availableCredit;
        private CustomerStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public Builder id(Long id) { this.id = id; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder dateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; return this; }
        public Builder monthlyIncome(Money monthlyIncome) { this.monthlyIncome = monthlyIncome; return this; }
        public Builder creditScore(Integer creditScore) { this.creditScore = creditScore; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder occupation(String occupation) { this.occupation = occupation; return this; }
        public Builder creditLimit(Money creditLimit) { this.creditLimit = creditLimit; return this; }
        public Builder availableCredit(Money availableCredit) { this.availableCredit = availableCredit; return this; }
        public Builder status(CustomerStatus status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public Customer build() {
            return new Customer(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                '}';
    }
}