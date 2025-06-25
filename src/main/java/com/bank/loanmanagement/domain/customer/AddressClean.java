package com.bank.loanmanagement.domain.customer;

import java.util.Objects;

/**
 * Clean Domain Model for Address - No Infrastructure Dependencies
 */
public class AddressClean {
    
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private AddressType type;
    private Boolean isPrimary;
    
    // Constructor
    public AddressClean(String street, String city, String state, 
                       String zipCode, String country, AddressType type) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.type = type;
        this.isPrimary = false;
    }
    
    // Default constructor
    public AddressClean() {
        this.isPrimary = false;
    }
    
    // Business methods
    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s", street, city, state, zipCode, country);
    }
    
    public boolean isInternational() {
        return country != null && !country.equalsIgnoreCase("USA") && !country.equalsIgnoreCase("United States");
    }
    
    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
    }
    
    // Getters
    public String getStreet() {
        return street;
    }
    
    public String getCity() {
        return city;
    }
    
    public String getState() {
        return state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    public AddressType getType() {
        return type;
    }
    
    public Boolean getIsPrimary() {
        return isPrimary;
    }
    
    // Setters
    public void setStreet(String street) {
        this.street = street;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public void setType(AddressType type) {
        this.type = type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressClean that = (AddressClean) o;
        return Objects.equals(street, that.street) &&
               Objects.equals(city, that.city) &&
               Objects.equals(state, that.state) &&
               Objects.equals(zipCode, that.zipCode) &&
               Objects.equals(country, that.country) &&
               type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, zipCode, country, type);
    }
    
    @Override
    public String toString() {
        return "AddressClean{" +
               "street='" + street + '\'' +
               ", city='" + city + '\'' +
               ", state='" + state + '\'' +
               ", zipCode='" + zipCode + '\'' +
               ", country='" + country + '\'' +
               ", type=" + type +
               ", isPrimary=" + isPrimary +
               '}';
    }
}