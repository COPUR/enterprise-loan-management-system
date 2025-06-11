package com.bank.loanmanagement.paymentprocessing.application;

import com.bank.loanmanagement.paymentprocessing.domain.Payment;
import com.bank.loanmanagement.paymentprocessing.infrastructure.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("payments", payments.stream().map(payment -> {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("id", payment.getId());
            paymentData.put("paymentNumber", payment.getPaymentNumber());
            paymentData.put("loanId", payment.getLoanId());
            paymentData.put("customerId", payment.getCustomerId());
            paymentData.put("paymentType", payment.getPaymentType());
            paymentData.put("scheduledAmount", payment.getScheduledAmount());
            paymentData.put("actualAmount", payment.getActualAmount());
            paymentData.put("principalAmount", payment.getPrincipalAmount());
            paymentData.put("interestAmount", payment.getInterestAmount());
            paymentData.put("penaltyAmount", payment.getPenaltyAmount());
            paymentData.put("scheduledDate", payment.getScheduledDate());
            paymentData.put("actualPaymentDate", payment.getActualPaymentDate());
            paymentData.put("paymentStatus", payment.getPaymentStatus());
            paymentData.put("paymentMethod", payment.getPaymentMethod());
            paymentData.put("transactionReference", payment.getTransactionReference());
            return paymentData;
        }).toList());
        
        response.put("total", payments.size());
        response.put("boundedContext", "Payment Processing (DDD)");
        response.put("businessRules", Map.of(
            "paymentTypes", List.of("REGULAR", "EARLY", "PARTIAL", "LATE"),
            "paymentMethods", List.of("BANK_TRANSFER", "ACH", "WIRE", "CHECK", "CASH"),
            "statusFlow", "PENDING → PROCESSING → COMPLETED/FAILED"
        ));
        response.put("dataSource", "PostgreSQL Database");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable Long id) {
        Optional<Payment> paymentOpt = paymentRepository.findById(id);
        
        if (paymentOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Payment not found");
            errorResponse.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Payment payment = paymentOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", payment.getId());
        response.put("paymentNumber", payment.getPaymentNumber());
        response.put("loanId", payment.getLoanId());
        response.put("customerId", payment.getCustomerId());
        response.put("paymentType", payment.getPaymentType());
        response.put("scheduledAmount", payment.getScheduledAmount());
        response.put("actualAmount", payment.getActualAmount());
        response.put("principalAmount", payment.getPrincipalAmount());
        response.put("interestAmount", payment.getInterestAmount());
        response.put("penaltyAmount", payment.getPenaltyAmount());
        response.put("scheduledDate", payment.getScheduledDate());
        response.put("actualPaymentDate", payment.getActualPaymentDate());
        response.put("paymentStatus", payment.getPaymentStatus());
        response.put("paymentMethod", payment.getPaymentMethod());
        response.put("transactionReference", payment.getTransactionReference());
        response.put("processorReference", payment.getProcessorReference());
        response.put("failureReason", payment.getFailureReason());
        response.put("totalAmount", payment.getTotalAmount());
        response.put("createdAt", payment.getCreatedAt());
        response.put("updatedAt", payment.getUpdatedAt());
        response.put("boundedContext", "Payment Processing (DDD)");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody Map<String, Object> paymentData) {
        try {
            Payment payment = new Payment();
            payment.setPaymentNumber("PAY" + System.currentTimeMillis());
            payment.setLoanId(Long.valueOf(paymentData.get("loanId").toString()));
            payment.setCustomerId(Long.valueOf(paymentData.get("customerId").toString()));
            payment.setScheduledAmount(new BigDecimal(paymentData.get("scheduledAmount").toString()));
            payment.setScheduledDate(LocalDate.parse(paymentData.get("scheduledDate").toString()));
            
            if (paymentData.containsKey("paymentType")) {
                payment.setPaymentType(paymentData.get("paymentType").toString());
            }
            
            if (paymentData.containsKey("paymentMethod")) {
                payment.setPaymentMethod(paymentData.get("paymentMethod").toString());
            }
            
            Payment savedPayment = paymentRepository.save(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedPayment.getId());
            response.put("paymentNumber", savedPayment.getPaymentNumber());
            response.put("loanId", savedPayment.getLoanId());
            response.put("scheduledAmount", savedPayment.getScheduledAmount());
            response.put("scheduledDate", savedPayment.getScheduledDate());
            response.put("paymentStatus", savedPayment.getPaymentStatus());
            response.put("message", "Payment scheduled successfully");
            response.put("boundedContext", "Payment Processing (DDD)");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{id}/process")
    public ResponseEntity<Map<String, Object>> processPayment(@PathVariable Long id, 
                                                             @RequestBody Map<String, Object> paymentData) {
        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(id);
            if (paymentOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Payment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            Payment payment = paymentOpt.get();
            
            // Update payment status to PROCESSING
            payment.setPaymentStatus("PROCESSING");
            
            // Simulate payment processing logic
            boolean paymentSuccess = Math.random() > 0.1; // 90% success rate simulation
            
            if (paymentSuccess) {
                BigDecimal actualAmount = new BigDecimal(paymentData.get("actualAmount").toString());
                String transactionRef = "TXN-" + System.currentTimeMillis();
                payment.markAsCompleted(actualAmount, transactionRef);
                
                if (paymentData.containsKey("paymentMethod")) {
                    payment.setPaymentMethod(paymentData.get("paymentMethod").toString());
                }
            } else {
                payment.markAsFailed("Insufficient funds or processing error");
            }
            
            Payment updatedPayment = paymentRepository.save(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedPayment.getId());
            response.put("paymentNumber", updatedPayment.getPaymentNumber());
            response.put("paymentStatus", updatedPayment.getPaymentStatus());
            response.put("actualAmount", updatedPayment.getActualAmount());
            response.put("actualPaymentDate", updatedPayment.getActualPaymentDate());
            response.put("transactionReference", updatedPayment.getTransactionReference());
            response.put("failureReason", updatedPayment.getFailureReason());
            response.put("message", paymentSuccess ? "Payment processed successfully" : "Payment processing failed");
            response.put("boundedContext", "Payment Processing (DDD)");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<Map<String, Object>> getPaymentsByLoanId(@PathVariable Long loanId) {
        List<Payment> payments = paymentRepository.findByLoanId(loanId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("payments", payments.stream().map(payment -> {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("id", payment.getId());
            paymentData.put("paymentNumber", payment.getPaymentNumber());
            paymentData.put("scheduledAmount", payment.getScheduledAmount());
            paymentData.put("actualAmount", payment.getActualAmount());
            paymentData.put("scheduledDate", payment.getScheduledDate());
            paymentData.put("actualPaymentDate", payment.getActualPaymentDate());
            paymentData.put("paymentStatus", payment.getPaymentStatus());
            paymentData.put("paymentType", payment.getPaymentType());
            return paymentData;
        }).toList());
        
        response.put("loanId", loanId);
        response.put("total", payments.size());
        response.put("boundedContext", "Payment Processing (DDD)");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/overdue")
    public ResponseEntity<Map<String, Object>> getOverduePayments() {
        List<Payment> overduePayments = paymentRepository.findOverduePayments();
        
        BigDecimal totalOverdue = overduePayments.stream()
            .map(Payment::getScheduledAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> response = new HashMap<>();
        response.put("overduePayments", overduePayments.stream().map(payment -> {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("id", payment.getId());
            paymentData.put("paymentNumber", payment.getPaymentNumber());
            paymentData.put("loanId", payment.getLoanId());
            paymentData.put("customerId", payment.getCustomerId());
            paymentData.put("scheduledAmount", payment.getScheduledAmount());
            paymentData.put("scheduledDate", payment.getScheduledDate());
            paymentData.put("daysOverdue", LocalDate.now().toEpochDay() - payment.getScheduledDate().toEpochDay());
            return paymentData;
        }).toList());
        
        response.put("totalOverdueAmount", totalOverdue);
        response.put("count", overduePayments.size());
        response.put("boundedContext", "Payment Processing (DDD)");
        response.put("dataSource", "PostgreSQL Database - Overdue Analysis");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPaymentStats() {
        long totalPayments = paymentRepository.count();
        List<Payment> pendingPayments = paymentRepository.findPendingPayments();
        List<Payment> overduePayments = paymentRepository.findOverduePayments();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalPayments", totalPayments);
        response.put("pendingPayments", pendingPayments.size());
        response.put("overduePayments", overduePayments.size());
        response.put("boundedContext", "Payment Processing (DDD)");
        response.put("dataSource", "PostgreSQL Database - Live Statistics");
        
        return ResponseEntity.ok(response);
    }
}