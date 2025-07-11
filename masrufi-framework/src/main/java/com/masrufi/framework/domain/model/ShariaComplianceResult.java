package com.masrufi.framework.domain.model;

import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of Sharia compliance validation
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Value
public class ShariaComplianceResult {
    
    boolean compliant;
    String message;
    List<String> violations;
    String validatingAuthority;
    LocalDateTime validationDate;

    public static ShariaComplianceResult compliant() {
        return new ShariaComplianceResult(
            true, 
            "Compliant with Sharia principles", 
            List.of(),
            "UAE_HIGHER_SHARIA_AUTHORITY",
            LocalDateTime.now()
        );
    }

    public static ShariaComplianceResult nonCompliant(String message) {
        return new ShariaComplianceResult(
            false, 
            message, 
            List.of(message),
            "UAE_HIGHER_SHARIA_AUTHORITY",
            LocalDateTime.now()
        );
    }
}