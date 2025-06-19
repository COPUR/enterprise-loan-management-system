package com.bank.loanmanagement.customermanagement.application;

import com.bank.loanmanagement.customermanagement.domain.Customer;
import com.bank.loanmanagement.customermanagement.infrastructure.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("customers", customers.stream().map(customer -> {
            Map<String, Object> customerData = new HashMap<>();
            customerData.put("id", customer.getId());
            customerData.put("customerNumber", customer.getCustomerNumber());
            customerData.put("name", customer.getFullName());
            customerData.put("email", customer.getEmail());
            customerData.put("creditScore", customer.getCreditScore());
            customerData.put("status", customer.getStatus());
            customerData.put("annualIncome", customer.getAnnualIncome());
            customerData.put("employmentStatus", customer.getEmploymentStatus());
            customerData.put("city", customer.getCity());
            customerData.put("state", customer.getState());
            return customerData;
        }).toList());
        
        response.put("total", customers.size());
        response.put("boundedContext", "Customer Management (DDD)");
        response.put("dataSource", "PostgreSQL Database");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Long id) {
        Optional<Customer> customerOpt = customerRepository.findById(id);
        
        if (customerOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Customer not found");
            errorResponse.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Customer customer = customerOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", customer.getId());
        response.put("customerNumber", customer.getCustomerNumber());
        response.put("firstName", customer.getFirstName());
        response.put("lastName", customer.getLastName());
        response.put("email", customer.getEmail());
        response.put("phoneNumber", customer.getPhoneNumber());
        response.put("dateOfBirth", customer.getDateOfBirth());
        response.put("creditScore", customer.getCreditScore());
        response.put("annualIncome", customer.getAnnualIncome());
        response.put("employmentStatus", customer.getEmploymentStatus());
        response.put("address", Map.of(
            "line1", customer.getAddressLine1(),
            "line2", customer.getAddressLine2(),
            "city", customer.getCity(),
            "state", customer.getState(),
            "zipCode", customer.getZipCode(),
            "country", customer.getCountry()
        ));
        response.put("status", customer.getStatus());
        response.put("createdAt", customer.getCreatedAt());
        response.put("updatedAt", customer.getUpdatedAt());
        response.put("boundedContext", "Customer Management (DDD)");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody Map<String, Object> customerData) {
        try {
            Customer customer = new Customer();
            customer.setCustomerNumber("CUST" + System.currentTimeMillis());
            customer.setFirstName((String) customerData.get("firstName"));
            customer.setLastName((String) customerData.get("lastName"));
            customer.setEmail((String) customerData.get("email"));
            customer.setPhoneNumber((String) customerData.get("phoneNumber"));
            customer.setSsn((String) customerData.get("ssn"));
            
            if (customerData.get("creditScore") != null) {
                customer.setCreditScore(Integer.valueOf(customerData.get("creditScore").toString()));
            }
            
            Customer savedCustomer = customerRepository.save(customer);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedCustomer.getId());
            response.put("customerNumber", savedCustomer.getCustomerNumber());
            response.put("name", savedCustomer.getFullName());
            response.put("email", savedCustomer.getEmail());
            response.put("status", savedCustomer.getStatus());
            response.put("message", "Customer created successfully");
            response.put("boundedContext", "Customer Management (DDD)");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create customer");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCustomerStats() {
        long totalCustomers = customerRepository.count();
        List<Customer> allCustomers = customerRepository.findAll();
        
        long activeCustomers = allCustomers.stream()
            .mapToLong(c -> "ACTIVE".equals(c.getStatus()) ? 1 : 0)
            .sum();
        
        double avgCreditScore = allCustomers.stream()
            .filter(c -> c.getCreditScore() != null)
            .mapToInt(Customer::getCreditScore)
            .average()
            .orElse(0.0);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalCustomers", totalCustomers);
        response.put("activeCustomers", activeCustomers);
        response.put("averageCreditScore", Math.round(avgCreditScore));
        response.put("boundedContext", "Customer Management (DDD)");
        response.put("dataSource", "PostgreSQL Database - Live Statistics");
        
        return ResponseEntity.ok(response);
    }
}