// bank-wide-service/domain/model/value/CustomerName.java
package com.loanmanagement.bankwide.domain.model.aggregate;

import java.util.Objects;

public final class CustomerName {
    private final String firstName;
    private final String lastName;
    
    private CustomerName(String firstName, String lastName) {
        this.firstName = validateName(firstName, "First name");
        this.lastName = validateName(lastName, "Last name");
    }
    
    public static CustomerName of(String firstName, String lastName) {
        return new CustomerName(firstName, lastName);
    }
    
    private String validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 100 characters");
        }
        return name.trim();
    }
    
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerName that = (CustomerName) o;
        return Objects.equals(firstName, that.firstName) && 
               Objects.equals(lastName, that.lastName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }
}