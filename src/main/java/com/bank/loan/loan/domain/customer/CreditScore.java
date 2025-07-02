package com.bank.loanmanagement.loan.domain.customer;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditScore {
    
    private Integer score;
    private String reportingAgency;
    private LocalDateTime lastUpdated;
    
    public boolean isExcellent() {
        return score != null && score >= 800;
    }
    
    public boolean isGood() {
        return score != null && score >= 670;
    }
    
    public boolean isFair() {
        return score != null && score >= 580;
    }
    
    public boolean isPoor() {
        return score != null && score < 580;
    }
    
    public String getRating() {
        if (isExcellent()) return "EXCELLENT";
        if (isGood()) return "GOOD";
        if (isFair()) return "FAIR";
        return "POOR";
    }
}