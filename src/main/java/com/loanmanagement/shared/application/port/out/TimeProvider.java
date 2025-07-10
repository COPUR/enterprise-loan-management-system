package com.loanmanagement.shared.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.Clock;

/**
 * Outbound Port for Time Provision
 * Abstracts time/date operations for better testability
 */
public interface TimeProvider {
    
    /**
     * Get current local date time
     */
    LocalDateTime now();
    
    /**
     * Get current local date
     */
    LocalDate today();
    
    /**
     * Get current zoned date time
     */
    ZonedDateTime nowZoned();
    
    /**
     * Get current timestamp as epoch milliseconds
     */
    long currentTimeMillis();
    
    /**
     * Get the clock instance
     */
    Clock getClock();
}