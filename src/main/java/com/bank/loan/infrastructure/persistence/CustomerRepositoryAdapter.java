package com.bank.loan.infrastructure.persistence;

import com.bank.loan.domain.model.*;
import com.bank.loan.domain.repository.CustomerDomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Customer Repository Adapter (Infrastructure Layer)
 * 
 * This adapter implements the domain repository interface and bridges
 * between the clean domain and the persistence infrastructure.
 */
@Component
public class CustomerRepositoryAdapter implements CustomerDomainRepository {

    private final CustomerJpaRepository jpaRepository;

    @Autowired
    public CustomerRepositoryAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = mapToEntity(customer);
        CustomerEntity savedEntity = jpaRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.getValue())
            .map(this::mapToDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .map(this::mapToDomain);
    }

    @Override
    public Optional<Customer> findByPhone(String phone) {
        return jpaRepository.findByPhone(phone)
            .map(this::mapToDomain);
    }

    @Override
    public List<Customer> findByStatus(CustomerStatus status) {
        return jpaRepository.findByStatus(status.name())
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findByCustomerType(CustomerType customerType) {
        return jpaRepository.findByCustomerType(customerType.name())
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findByCreditScoreRange(Integer minScore, Integer maxScore) {
        return jpaRepository.findByCreditScoreBetween(minScore, maxScore)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findLoanEligibleCustomers(Money requestedAmount) {
        // Get all customers with basic eligibility criteria
        List<CustomerEntity> basicEligibleCustomers = jpaRepository.findLoanEligibleCustomers();
        
        return basicEligibleCustomers.stream()
            .map(this::mapToDomain)
            .filter(customer -> isCustomerEligibleForLoanAmount(customer, requestedAmount))
            .collect(Collectors.toList());
    }
    
    /**
     * Comprehensive loan eligibility assessment for specific amount
     */
    private boolean isCustomerEligibleForLoanAmount(Customer customer, Money requestedAmount) {
        // 1. Basic status check
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            return false;
        }
        
        // 2. Credit score requirement (minimum 600 for any loan)
        if (customer.getCreditScore() == null || customer.getCreditScore() < 600) {
            return false;
        }
        
        // 3. Age requirements (18-70 years)
        if (customer.getAge() < 18 || customer.getAge() > 70) {
            return false;
        }
        
        // 4. Minimum income requirement (3x the annual loan payment)
        BigDecimal requestedAmountValue = requestedAmount.getAmount();
        BigDecimal minimumMonthlyIncome = calculateMinimumIncomeRequirement(requestedAmountValue);
        
        if (customer.getMonthlyIncome().compareTo(minimumMonthlyIncome) < 0) {
            return false;
        }
        
        // 5. Debt-to-income ratio check (max 40%)
        BigDecimal maxAllowableDebt = customer.getMonthlyIncome().multiply(new BigDecimal("0.40"));
        BigDecimal newLoanPayment = calculateMonthlyLoanPayment(requestedAmountValue);
        BigDecimal totalDebtWithNewLoan = customer.getProjectedTotalObligations().add(newLoanPayment);
        
        if (totalDebtWithNewLoan.compareTo(maxAllowableDebt) > 0) {
            return false;
        }
        
        // 6. Maximum loan amount based on credit score and income
        BigDecimal maxLoanAmount = calculateMaximumLoanAmount(customer);
        if (requestedAmountValue.compareTo(maxLoanAmount) > 0) {
            return false;
        }
        
        // 7. Employment stability check (minimum 2 years employment)
        if (!hasStableEmployment(customer)) {
            return false;
        }
        
        // 8. Existing loan count limit (max 3 active loans)
        if (getActiveLoansCount(customer) >= 3) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate minimum monthly income requirement
     */
    private BigDecimal calculateMinimumIncomeRequirement(BigDecimal loanAmount) {
        // Assume 5-year loan term at 6% interest rate
        BigDecimal monthlyPayment = calculateMonthlyLoanPayment(loanAmount);
        
        // Income should be at least 3x the monthly payment
        return monthlyPayment.multiply(new BigDecimal("3"));
    }
    
    /**
     * Calculate monthly loan payment (simplified calculation)
     */
    private BigDecimal calculateMonthlyLoanPayment(BigDecimal loanAmount) {
        // Simple calculation: 5-year term at 6% annual interest
        BigDecimal monthlyRate = new BigDecimal("0.06").divide(new BigDecimal("12"), 6, BigDecimal.ROUND_HALF_UP);
        int numberOfPayments = 60; // 5 years * 12 months
        
        // Payment = P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(numberOfPayments);
        
        BigDecimal numerator = loanAmount.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Calculate maximum loan amount based on customer profile
     */
    private BigDecimal calculateMaximumLoanAmount(Customer customer) {
        BigDecimal baseAmount;
        Integer creditScore = customer.getCreditScore();
        
        // Base amount calculation based on credit score
        if (creditScore >= 800) {
            baseAmount = customer.getMonthlyIncome().multiply(new BigDecimal("120")); // 10 years income
        } else if (creditScore >= 750) {
            baseAmount = customer.getMonthlyIncome().multiply(new BigDecimal("96")); // 8 years income
        } else if (creditScore >= 700) {
            baseAmount = customer.getMonthlyIncome().multiply(new BigDecimal("72")); // 6 years income
        } else if (creditScore >= 650) {
            baseAmount = customer.getMonthlyIncome().multiply(new BigDecimal("48")); // 4 years income
        } else {
            baseAmount = customer.getMonthlyIncome().multiply(new BigDecimal("24")); // 2 years income
        }
        
        // Apply caps based on customer type and risk factors
        BigDecimal maxCap = new BigDecimal("1000000"); // 1M AED maximum
        BigDecimal minCap = new BigDecimal("10000");   // 10K AED minimum
        
        // Adjust for existing obligations
        BigDecimal availableCapacity = customer.getMonthlyIncome()
            .multiply(new BigDecimal("0.40"))
            .subtract(customer.getProjectedTotalObligations())
            .multiply(new BigDecimal("60")); // Convert to 5-year loan capacity
        
        return baseAmount
            .min(maxCap)
            .min(availableCapacity)
            .max(minCap);
    }
    
    /**
     * Check employment stability
     */
    private boolean hasStableEmployment(Customer customer) {
        // Simplified check - in reality would check employment history
        // For now, assume customers with higher income have stable employment
        return customer.getMonthlyIncome().compareTo(new BigDecimal("5000")) >= 0;
    }
    
    /**
     * Get count of active loans for customer
     */
    private int getActiveLoansCount(Customer customer) {
        // Simplified check - would typically query loan repository
        // For now, estimate based on total obligations
        BigDecimal avgLoanPayment = new BigDecimal("2000");
        return customer.getProjectedTotalObligations()
            .divide(avgLoanPayment, 0, BigDecimal.ROUND_DOWN)
            .intValue();
    }

    @Override
    public List<Customer> findActiveCustomers() {
        return jpaRepository.findActiveCustomers()
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findIslamicBankingCustomers() {
        return jpaRepository.findByIslamicBankingPreference(true)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaRepository.existsByPhone(phone);
    }

    @Override
    public void deleteById(CustomerId customerId) {
        jpaRepository.findByCustomerId(customerId.getValue())
            .ifPresent(entity -> jpaRepository.deleteById(entity.getId()));
    }

    @Override
    public long countByStatus(CustomerStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public List<Customer> findByLocation(String city, String country) {
        return jpaRepository.findByCityAndCountry(city, country)
            .stream()
            .map(this::mapToDomain)
            .collect(Collectors.toList());
    }

    // Mapping methods

    private CustomerEntity mapToEntity(Customer customer) {
        if (customer == null) return null;

        return CustomerEntity.builder()
            .customerId(customer.getCustomerId() != null ? customer.getCustomerId().getValue() : null)
            .firstName(customer.getFirstName())
            .lastName(customer.getLastName())
            .email(customer.getEmail())
            .phone(customer.getPhone())
            .address(customer.getAddress())
            .city(customer.getCity())
            .postalCode(customer.getPostalCode())
            .country(customer.getCountry())
            .nationality(customer.getNationality())
            .occupation(customer.getOccupation())
            .employerName(customer.getEmployerName())
            .dateOfBirth(customer.getDateOfBirth())
            .creditScore(customer.getCreditScore())
            .creditLimit(customer.getCreditLimit())
            .monthlyIncome(customer.getMonthlyIncome())
            .existingMonthlyObligations(customer.getExistingMonthlyObligations())
            .status(customer.getStatus() != null ? customer.getStatus().name() : null)
            .riskCategory(customer.getRiskCategory() != null ? customer.getRiskCategory().name() : null)
            .customerType(customer.getCustomerType() != null ? customer.getCustomerType().name() : null)
            .employmentType(customer.getEmploymentType() != null ? customer.getEmploymentType().name() : null)
            .drivingLicense(customer.getDrivingLicense())
            .businessLicense(customer.getBusinessLicense())
            .businessName(customer.getBusinessName())
            .yearsInBusiness(customer.getYearsInBusiness())
            .religionPreference(customer.getReligionPreference())
            .islamicBankingPreference(customer.getIslamicBankingPreference())
            .registrationDate(customer.getRegistrationDate())
            .lastUpdated(customer.getLastUpdated())
            .useCaseReference(customer.getUseCaseReference())
            .build();
    }

    private Customer mapToDomain(CustomerEntity entity) {
        if (entity == null) return null;

        Customer customer = Customer.builder()
            .customerId(entity.getCustomerId() != null ? new CustomerId(entity.getCustomerId()) : null)
            .firstName(entity.getFirstName())
            .lastName(entity.getLastName())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .address(entity.getAddress())
            .city(entity.getCity())
            .postalCode(entity.getPostalCode())
            .country(entity.getCountry())
            .nationality(entity.getNationality())
            .occupation(entity.getOccupation())
            .employerName(entity.getEmployerName())
            .dateOfBirth(entity.getDateOfBirth())
            .creditScore(entity.getCreditScore())
            .creditLimit(entity.getCreditLimit())
            .monthlyIncome(entity.getMonthlyIncome())
            .existingMonthlyObligations(entity.getExistingMonthlyObligations())
            .status(entity.getStatus() != null ? CustomerStatus.valueOf(entity.getStatus()) : null)
            .riskCategory(entity.getRiskCategory() != null ? RiskCategory.valueOf(entity.getRiskCategory()) : null)
            .customerType(entity.getCustomerType() != null ? CustomerType.valueOf(entity.getCustomerType()) : null)
            .employmentType(entity.getEmploymentType() != null ? EmploymentType.valueOf(entity.getEmploymentType()) : null)
            .drivingLicense(entity.getDrivingLicense())
            .businessLicense(entity.getBusinessLicense())
            .businessName(entity.getBusinessName())
            .yearsInBusiness(entity.getYearsInBusiness())
            .religionPreference(entity.getReligionPreference())
            .islamicBankingPreference(entity.getIslamicBankingPreference())
            .registrationDate(entity.getRegistrationDate())
            .lastUpdated(entity.getLastUpdated())
            .useCaseReference(entity.getUseCaseReference())
            .build();

        return customer;
    }
}