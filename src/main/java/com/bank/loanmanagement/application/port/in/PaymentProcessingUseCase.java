
package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.application.dto.PaymentRequest;
import com.bank.loanmanagement.application.dto.PaymentResponse;
import com.bank.loanmanagement.domain.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentProcessingUseCase {
    
    PaymentResponse createPayment(PaymentRequest request);
    
    PaymentResponse processPayment(Long paymentId);
    
    PaymentResponse completePayment(Long paymentId);
    
    PaymentResponse failPayment(Long paymentId, String reason);
    
    PaymentResponse getPaymentById(Long paymentId);
    
    PaymentResponse getPaymentByTransactionReference(String transactionReference);
    
    List<PaymentResponse> getPaymentsByLoanId(Long loanId);
    
    List<PaymentResponse> getPaymentsByCustomerId(Long customerId);
    
    List<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status);
    
    List<PaymentResponse> getPendingPayments();
    
    List<PaymentResponse> getCompletedPayments();
    
    List<PaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    BigDecimal getTotalPaymentsForLoan(Long loanId);
    
    BigDecimal getTotalPaymentsForCustomer(Long customerId);
    
    BigDecimal getTotalPaymentAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    boolean refundPayment(Long paymentId);
    
    boolean cancelPayment(Long paymentId);
    
    void processScheduledPayments();
}
