package com.bank.loanmanagement.customermanagement.domain.port.in;

import java.util.Objects;

/**
 * Query for getting all customers with pagination support.
 */
public record GetAllCustomersQuery(
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    
    public GetAllCustomersQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
        
        Objects.requireNonNull(sortBy, "Sort by field is required");
        Objects.requireNonNull(sortDirection, "Sort direction is required");
        
        if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }
    }
    
    public static GetAllCustomersQuery defaultQuery() {
        return new GetAllCustomersQuery(0, 20, "createdAt", "desc");
    }
}