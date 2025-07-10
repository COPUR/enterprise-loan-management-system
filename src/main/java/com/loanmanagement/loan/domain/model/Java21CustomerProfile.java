package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;

/**
 * Java 21 Enhanced Customer Profile using modern patterns
 * 
 * Features demonstrated:
 * - Sealed interfaces for type-safe customer classification
 * - Record patterns for customer data structures
 * - Pattern matching for risk assessment
 * - Enhanced switch expressions for business logic
 */
public sealed interface Java21CustomerProfile 
    permits PremiumCustomer, StandardCustomer, YoungProfessional, BusinessCustomer, RetiredCustomer {
    
    // Common properties
    CustomerId customerId();
    String fullName();
    int creditScore();
    Money monthlyIncome();
    LocalDate dateOfBirth();
    
    // Default methods using pattern matching
    default int getAge() {
        return Period.between(dateOfBirth(), LocalDate.now()).getYears();
    }
    
    default CreditCategory getCreditCategory() {
        int score = creditScore();
        if (score >= 800) {
            return CreditCategory.EXCEPTIONAL;
        } else if (score >= 740) {
            return CreditCategory.VERY_GOOD;
        } else if (score >= 670) {
            return CreditCategory.GOOD;
        } else if (score >= 580) {
            return CreditCategory.FAIR;
        } else {
            return CreditCategory.POOR;
        }
    }
    
    default RiskAssessment calculateRiskAssessment() {
        return switch (this) {
            case PremiumCustomer(var id, var name, var creditScore, var income, var dob, var relationshipYears, var assets) 
                when creditScore >= 750 && income.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0 -> {
                yield new RiskAssessment(
                    RiskLevel.LOW,
                    0.95,
                    "Premium customer with excellent credit and high income",
                    List.of("High credit score", "High income", "Long relationship"),
                    calculatePremiumInterestRate(creditScore, income)
                );
            }
            
            case StandardCustomer(var id, var name, var creditScore, var income, var dob, var employment, var tenure) 
                when creditScore >= 650 && isStableEmployment(employment) -> {
                yield new RiskAssessment(
                    RiskLevel.MEDIUM,
                    0.75,
                    "Standard customer with good creditworthiness",
                    List.of("Good credit score", "Stable employment"),
                    calculateStandardInterestRate(creditScore, employment)
                );
            }
            
            case YoungProfessional(var id, var name, var creditScore, var income, var dob, var education, var profession) 
                when getAge() < 35 && hasHighEducation(education) -> {
                yield new RiskAssessment(
                    RiskLevel.MEDIUM,
                    0.70,
                    "Young professional with growth potential",
                    List.of("Higher education", "Professional career", "Growth potential"),
                    calculateYoungProfessionalRate(education, profession)
                );
            }
            
            case BusinessCustomer(var id, var name, var creditScore, var income, var dob, var businessType, var yearsInBusiness, var revenue) 
                when yearsInBusiness >= 3 && revenue.getAmount().compareTo(BigDecimal.valueOf(500000)) > 0 -> {
                yield new RiskAssessment(
                    calculateBusinessRiskLevel(businessType),
                    0.80,
                    "Established business customer",
                    List.of("Established business", "Good revenue"),
                    calculateBusinessRate(businessType, revenue)
                );
            }
            
            case RetiredCustomer(var id, var name, var creditScore, var income, var dob, var pensionType, var assets) 
                when creditScore >= 700 && assets.getAmount().compareTo(BigDecimal.valueOf(250000)) > 0 -> {
                yield new RiskAssessment(
                    RiskLevel.LOW,
                    0.85,
                    "Retired customer with substantial assets",
                    List.of("Good credit history", "Substantial assets", "Stable pension"),
                    calculateRetiredCustomerRate(pensionType, assets)
                );
            }
            
            default -> new RiskAssessment(
                RiskLevel.HIGH,
                0.45,
                "Standard risk assessment required",
                List.of("Requires manual review"),
                BigDecimal.valueOf(8.5)
            );
        };
    }
    
    default LoanEligibility checkLoanEligibility(Money requestedAmount, LoanPurpose purpose) {
        return switch (this) {
            case PremiumCustomer premium when premium.creditScore() >= 750 -> {
                var maxAmount = premium.monthlyIncome().getAmount().multiply(BigDecimal.valueOf(60)); // 5 years income
                var eligible = requestedAmount.getAmount().compareTo(maxAmount) <= 0;
                yield new LoanEligibility(
                    eligible,
                    eligible ? maxAmount : requestedAmount.getAmount(),
                    eligible ? "Pre-approved for premium customer" : "Exceeds maximum loan amount",
                    calculatePremiumInterestRate(premium.creditScore(), premium.monthlyIncome())
                );
            }
            
            case StandardCustomer standard when standard.creditScore() >= 650 -> {
                var maxAmount = standard.monthlyIncome().getAmount().multiply(BigDecimal.valueOf(36)); // 3 years income
                var eligible = requestedAmount.getAmount().compareTo(maxAmount) <= 0;
                yield new LoanEligibility(
                    eligible,
                    eligible ? maxAmount : requestedAmount.getAmount(),
                    eligible ? "Eligible for standard loan terms" : "Exceeds standard loan limit",
                    calculateStandardInterestRate(standard.creditScore(), standard.employmentType())
                );
            }
            
            case YoungProfessional young when young.creditScore() >= 600 -> {
                var maxAmount = young.monthlyIncome().getAmount().multiply(BigDecimal.valueOf(30));
                var eligible = requestedAmount.getAmount().compareTo(maxAmount) <= 0;
                yield new LoanEligibility(
                    eligible,
                    eligible ? maxAmount : requestedAmount.getAmount(),
                    eligible ? "Eligible with young professional program" : "Exceeds young professional limit",
                    calculateYoungProfessionalRate(young.educationLevel(), young.profession())
                );
            }
            
            case BusinessCustomer business when business.yearsInBusiness() >= 2 -> {
                var maxAmount = business.annualRevenue().getAmount().multiply(BigDecimal.valueOf(2));
                var eligible = requestedAmount.getAmount().compareTo(maxAmount) <= 0;
                yield new LoanEligibility(
                    eligible,
                    eligible ? maxAmount : requestedAmount.getAmount(),
                    eligible ? "Eligible for business loan" : "Exceeds business loan capacity",
                    calculateBusinessRate(business.businessType(), business.annualRevenue())
                );
            }
            
            default -> new LoanEligibility(
                false,
                BigDecimal.ZERO,
                "Does not meet minimum eligibility criteria",
                BigDecimal.valueOf(12.0)
            );
        };
    }
    
    // Helper methods
    private boolean isStableEmployment(EmploymentType type) {
        return type == EmploymentType.FULL_TIME || type == EmploymentType.GOVERNMENT;
    }
    
    private boolean hasHighEducation(EducationLevel education) {
        return education == EducationLevel.GRADUATE || education == EducationLevel.POST_GRADUATE;
    }
    
    private RiskLevel calculateBusinessRiskLevel(BusinessType type) {
        return switch (type) {
            case TECHNOLOGY, HEALTHCARE -> RiskLevel.LOW;
            case MANUFACTURING, PROFESSIONAL_SERVICES -> RiskLevel.MEDIUM;
            case RETAIL, HOSPITALITY -> RiskLevel.HIGH;
        };
    }
    
    private BigDecimal calculatePremiumInterestRate(int creditScore, Money income) {
        var baseRate = BigDecimal.valueOf(3.5);
        var creditAdjustment = BigDecimal.valueOf((800 - creditScore) * 0.01);
        var incomeBonus = income.getAmount().compareTo(BigDecimal.valueOf(200000)) > 0 ? 
            BigDecimal.valueOf(-0.25) : BigDecimal.ZERO;
        return baseRate.add(creditAdjustment).add(incomeBonus);
    }
    
    private BigDecimal calculateStandardInterestRate(int creditScore, EmploymentType employment) {
        var baseRate = BigDecimal.valueOf(5.5);
        var creditAdjustment = BigDecimal.valueOf((750 - creditScore) * 0.015);
        var employmentBonus = employment == EmploymentType.GOVERNMENT ? 
            BigDecimal.valueOf(-0.5) : BigDecimal.ZERO;
        return baseRate.add(creditAdjustment).add(employmentBonus);
    }
    
    private BigDecimal calculateYoungProfessionalRate(EducationLevel education, Profession profession) {
        var baseRate = BigDecimal.valueOf(6.0);
        var educationBonus = education == EducationLevel.POST_GRADUATE ? 
            BigDecimal.valueOf(-0.5) : BigDecimal.ZERO;
        var professionBonus = switch (profession) {
            case DOCTOR, LAWYER, ENGINEER -> BigDecimal.valueOf(-0.75);
            case TEACHER, NURSE -> BigDecimal.valueOf(-0.25);
            default -> BigDecimal.ZERO;
        };
        return baseRate.add(educationBonus).add(professionBonus);
    }
    
    private BigDecimal calculateBusinessRate(BusinessType type, Money revenue) {
        var baseRate = BigDecimal.valueOf(7.0);
        var typeAdjustment = switch (type) {
            case TECHNOLOGY -> BigDecimal.valueOf(-1.0);
            case HEALTHCARE -> BigDecimal.valueOf(-0.5);
            case MANUFACTURING -> BigDecimal.ZERO;
            case RETAIL -> BigDecimal.valueOf(0.5);
            case HOSPITALITY -> BigDecimal.valueOf(1.0);
            case PROFESSIONAL_SERVICES -> BigDecimal.valueOf(-0.25);
        };
        var revenueBonus = revenue.getAmount().compareTo(BigDecimal.valueOf(1000000)) > 0 ? 
            BigDecimal.valueOf(-0.5) : BigDecimal.ZERO;
        return baseRate.add(typeAdjustment).add(revenueBonus);
    }
    
    private BigDecimal calculateRetiredCustomerRate(PensionType pensionType, Money assets) {
        var baseRate = BigDecimal.valueOf(4.5);
        var pensionBonus = pensionType == PensionType.GOVERNMENT ? 
            BigDecimal.valueOf(-0.5) : BigDecimal.ZERO;
        var assetBonus = assets.getAmount().compareTo(BigDecimal.valueOf(500000)) > 0 ? 
            BigDecimal.valueOf(-0.25) : BigDecimal.ZERO;
        return baseRate.add(pensionBonus).add(assetBonus);
    }
}

// Sealed customer types using Records
record PremiumCustomer(
    CustomerId customerId,
    String fullName,
    int creditScore,
    Money monthlyIncome,
    LocalDate dateOfBirth,
    int relationshipYears,
    Money totalAssets
) implements Java21CustomerProfile {}

record StandardCustomer(
    CustomerId customerId,
    String fullName,
    int creditScore,
    Money monthlyIncome,
    LocalDate dateOfBirth,
    EmploymentType employmentType,
    int employmentTenureMonths
) implements Java21CustomerProfile {}

record YoungProfessional(
    CustomerId customerId,
    String fullName,
    int creditScore,
    Money monthlyIncome,
    LocalDate dateOfBirth,
    EducationLevel educationLevel,
    Profession profession
) implements Java21CustomerProfile {}

record BusinessCustomer(
    CustomerId customerId,
    String fullName,
    int creditScore,
    Money monthlyIncome,
    LocalDate dateOfBirth,
    BusinessType businessType,
    int yearsInBusiness,
    Money annualRevenue
) implements Java21CustomerProfile {}

record RetiredCustomer(
    CustomerId customerId,
    String fullName,
    int creditScore,
    Money monthlyIncome,
    LocalDate dateOfBirth,
    PensionType pensionType,
    Money totalAssets
) implements Java21CustomerProfile {}

// Supporting enums and records
enum CreditCategory {
    EXCEPTIONAL, VERY_GOOD, GOOD, FAIR, POOR
}

enum EducationLevel {
    HIGH_SCHOOL, UNDERGRADUATE, GRADUATE, POST_GRADUATE
}

enum Profession {
    DOCTOR, LAWYER, ENGINEER, TEACHER, NURSE, MANAGER, ANALYST, OTHER
}

enum BusinessType {
    TECHNOLOGY, HEALTHCARE, MANUFACTURING, RETAIL, HOSPITALITY, PROFESSIONAL_SERVICES
}

enum PensionType {
    GOVERNMENT, PRIVATE, SOCIAL_SECURITY, INVESTMENT
}

record RiskAssessment(
    RiskLevel riskLevel,
    double confidence,
    String reasoning,
    List<String> riskFactors,
    BigDecimal recommendedInterestRate
) {}

record LoanEligibility(
    boolean eligible,
    BigDecimal maxLoanAmount,
    String reason,
    BigDecimal interestRate
) {}