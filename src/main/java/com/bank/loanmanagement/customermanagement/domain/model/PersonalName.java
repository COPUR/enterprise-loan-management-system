package com.bank.loanmanagement.customermanagement.domain.model;

import java.util.Objects;

/**
 * Value object representing a person's name.
 * Encapsulates validation rules for personal names.
 */
public final class PersonalName {
    
    private final String firstName;
    private final String lastName;
    
    private PersonalName(String firstName, String lastName) {
        this.firstName = validateName(firstName, "First name");
        this.lastName = validateName(lastName, "Last name");
    }
    
    public static PersonalName of(String firstName, String lastName) {
        return new PersonalName(firstName, lastName);
    }
    
    private String validateName(String name, String fieldName) {
        Objects.requireNonNull(name, fieldName + " cannot be null");
        String trimmed = name.trim();
        
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 50 characters");
        }
        
        if (!trimmed.matches("^[a-zA-Z\\s'-]+$")) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }
        
        return trimmed;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalName that = (PersonalName) o;
        return Objects.equals(firstName, that.firstName) && 
               Objects.equals(lastName, that.lastName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName);
    }
    
    @Override
    public String toString() {
        return getFullName();
    }
}