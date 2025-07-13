package com.amanahfi.onboarding.domain.customer;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Business Details Value Object
 * Contains business-specific information for corporate customers
 */
@Embeddable
@Getter
@NoArgsConstructor
public class BusinessDetails {
    
    private String businessName;
    private String tradeLicenseNumber;
    private LocalDate establishmentDate;
    private String authorizedPersonEmiratesId;
    
    // Additional business fields can be added here
    private String businessActivity;
    private String companySize;
    private String annualRevenue;
    
    public BusinessDetails(String businessName, String tradeLicenseNumber, 
                          LocalDate establishmentDate, String authorizedPersonEmiratesId) {
        this.businessName = businessName;
        this.tradeLicenseNumber = tradeLicenseNumber;
        this.establishmentDate = establishmentDate;
        this.authorizedPersonEmiratesId = authorizedPersonEmiratesId;
    }
}