package com.bank.loan.infrastructure.persistence;

import com.bank.loan.domain.model.*;
import com.bank.loan.domain.repository.CustomerDomainRepository;
import com.bank.loan.domain.service.LoanEligibilityService;
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
    private final LoanEligibilityService loanEligibilityService;

    @Autowired
    public CustomerRepositoryAdapter(CustomerJpaRepository jpaRepository, 
                                   LoanEligibilityService loanEligibilityService) {
        this.jpaRepository = jpaRepository;
        this.loanEligibilityService = loanEligibilityService;
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
        
        // Delegate eligibility assessment to domain service
        return basicEligibleCustomers.stream()
            .map(this::mapToDomain)
            .filter(customer -> loanEligibilityService.assessEligibility(customer, requestedAmount).isApproved())
            .collect(Collectors.toList());
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