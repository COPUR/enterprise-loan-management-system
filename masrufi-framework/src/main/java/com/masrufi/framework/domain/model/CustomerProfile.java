package com.masrufi.framework.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Customer Profile value object
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class CustomerProfile {
    
    String customerId;
    String customerName;
    CustomerType customerType;
    Integer creditScore;
    Money monthlyIncome;
    String jurisdiction;
}

enum CustomerType {
    INDIVIDUAL,
    CORPORATE,
    GOVERNMENT,
    NON_PROFIT
}