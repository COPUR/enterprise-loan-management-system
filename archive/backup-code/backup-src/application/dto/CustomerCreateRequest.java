
package com.bank.loanmanagement.application.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateRequest {
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String name;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "1000.00", message = "Minimum credit limit is 1000.00")
    @DecimalMax(value = "10000000.00", message = "Maximum credit limit is 10,000,000.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal creditLimit;
}
