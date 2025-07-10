package com.masrufi.sharia.service;

import com.masrufi.sharia.domain.model.*;
import com.masrufi.sharia.repository.IslamicFinancingRepository;
import com.masrufi.sharia.repository.PaymentScheduleRepository;
import com.masrufi.sharia.repository.ComplianceCheckRepository;
import com.masrufi.sharia.dto.FinancingApplicationRequest;
import com.masrufi.sharia.dto.FinancingResponse;
import com.masrufi.sharia.dto.PaymentRequest;
import com.masrufi.sharia.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Sharia Financing Service
 * Core business logic for Islamic financing operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShariaFinancingService {
    
    private final IslamicFinancingRepository financingRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final ComplianceCheckRepository complianceCheckRepository;
    private final ShariaComplianceService complianceService;
    private final PaymentProcessingService paymentService;
    private final BlockchainService blockchainService;
    
    /**
     * Apply for Islamic financing
     */
    public FinancingResponse applyForFinancing(FinancingApplicationRequest request) {
        log.info("Processing Islamic financing application for customer: {}", request.getCustomerId());
        
        // 1. Validate Sharia compliance
        var complianceResult = complianceService.validateFinancingRequest(request);
        if (!complianceResult.isCompliant()) {
            throw new ShariaComplianceException("Application does not meet Sharia requirements: " + 
                complianceResult.getReason());
        }
        
        // 2. Create financing entity
        var financing = IslamicFinancing.builder()
            .financingReference(generateFinancingReference())
            .customerId(request.getCustomerId())
            .financingType(request.getFinancingType())
            .principalAmount(request.getPrincipalAmount())
            .paymentFrequency(request.getPaymentFrequency())
            .startDate(request.getStartDate())
            .maturityDate(calculateMaturityDate(request))
            .isAssetBacked(request.getIsAssetBacked())
            .assetDetails(request.getAssetDetails())
            .blockchainNetwork(request.getBlockchainNetwork())
            .cryptoCurrency(request.getCryptoCurrency())
            .cryptoAmount(request.getCryptoAmount())
            .status(FinancingStatus.PENDING_APPROVAL)
            .createdBy(request.getApplicantId())
            .build();
        
        // 3. Calculate profit amount
        var profitAmount = calculateProfitAmount(financing);
        financing.setProfitAmount(profitAmount);
        financing.setTotalAmount(financing.getPrincipalAmount().add(profitAmount));
        
        // 4. Save financing
        financing = financingRepository.save(financing);
        
        // 5. Create payment schedule
        createPaymentSchedule(financing);
        
        // 6. Deploy smart contract
        if (request.getUseSmartContract()) {
            var contractAddress = blockchainService.deployFinancingContract(financing);
            financing.setSmartContractAddress(contractAddress);
            financingRepository.save(financing);
        }
        
        // 7. Submit to Sharia board for approval
        submitForShariaApproval(financing, complianceResult);
        
        log.info("Islamic financing application created with reference: {}", 
            financing.getFinancingReference());
        
        return mapToResponse(financing);
    }
    
    /**
     * Process payment for financing
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for financing: {}", request.getFinancingReference());
        
        var financing = financingRepository.findByFinancingReference(request.getFinancingReference())
            .orElseThrow(() -> new FinancingNotFoundException("Financing not found: " + 
                request.getFinancingReference()));
        
        // 1. Validate payment
        validatePayment(financing, request);
        
        // 2. Find next due payment
        var nextPayment = paymentScheduleRepository.findNextDuePayment(financing.getId())
            .orElseThrow(() -> new PaymentException("No payment due for this financing"));
        
        // 3. Process blockchain transaction
        var transactionResult = paymentService.processBlockchainPayment(request);
        
        // 4. Update payment schedule
        nextPayment.markAsPaid(request.getAmount(), transactionResult.getTransactionHash());
        paymentScheduleRepository.save(nextPayment);
        
        // 5. Update financing status
        updateFinancingAfterPayment(financing);
        
        // 6. Send notifications
        // TODO: Implement notification service
        
        log.info("Payment processed successfully for financing: {}", 
            request.getFinancingReference());
        
        return PaymentResponse.builder()
            .paymentId(nextPayment.getId())
            .financingReference(financing.getFinancingReference())
            .amount(request.getAmount())
            .transactionHash(transactionResult.getTransactionHash())
            .status("COMPLETED")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Get financing by reference
     */
    @Transactional(readOnly = true)
    public FinancingResponse getFinancing(String financingReference) {
        var financing = financingRepository.findByFinancingReference(financingReference)
            .orElseThrow(() -> new FinancingNotFoundException("Financing not found: " + 
                financingReference));
        
        return mapToResponse(financing);
    }
    
    /**
     * Get customer financings
     */
    @Transactional(readOnly = true)
    public List<FinancingResponse> getCustomerFinancings(String customerId) {
        var financings = financingRepository.findByCustomerId(customerId);
        return financings.stream()
            .map(this::mapToResponse)
            .toList();
    }
    
    /**
     * Get payment schedule for financing
     */
    @Transactional(readOnly = true)
    public List<PaymentSchedule> getPaymentSchedule(String financingReference) {
        var financing = financingRepository.findByFinancingReference(financingReference)
            .orElseThrow(() -> new FinancingNotFoundException("Financing not found: " + 
                financingReference));
        
        return paymentScheduleRepository.findByFinancingIdOrderByInstallmentNumber(financing.getId());
    }
    
    // Private helper methods
    
    private String generateFinancingReference() {
        return "MF-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private LocalDateTime calculateMaturityDate(FinancingApplicationRequest request) {
        var startDate = request.getStartDate();
        var termInDays = request.getTermInDays();
        
        return startDate.plusDays(termInDays);
    }
    
    private BigDecimal calculateProfitAmount(IslamicFinancing financing) {
        // This would be more sophisticated in real implementation
        // considering market rates, risk factors, etc.
        return financing.calculateProfitAmount();
    }
    
    private void createPaymentSchedule(IslamicFinancing financing) {
        var numberOfPayments = calculateNumberOfPayments(financing);
        var paymentAmount = financing.getTotalAmount().divide(
            BigDecimal.valueOf(numberOfPayments), 2, BigDecimal.ROUND_HALF_UP);
        
        var paymentDate = financing.getStartDate();
        var increment = getPaymentIncrement(financing.getPaymentFrequency());
        
        for (int i = 1; i <= numberOfPayments; i++) {
            paymentDate = paymentDate.plus(increment.getValue(), increment.getUnit());
            
            var payment = PaymentSchedule.builder()
                .financing(financing)
                .installmentNumber(i)
                .amount(paymentAmount)
                .dueDate(paymentDate)
                .status(PaymentStatus.PENDING)
                .build();
            
            paymentScheduleRepository.save(payment);
        }
        
        // Update next payment date
        financing.setNextPaymentDate(financing.getStartDate().plus(increment.getValue(), increment.getUnit()));
    }
    
    private int calculateNumberOfPayments(IslamicFinancing financing) {
        var totalDays = ChronoUnit.DAYS.between(financing.getStartDate(), financing.getMaturityDate());
        
        return switch (financing.getPaymentFrequency()) {
            case HOURLY -> (int) (totalDays * 24);
            case DAILY -> (int) totalDays;
            case WEEKLY -> (int) (totalDays / 7);
            case MONTHLY -> (int) (totalDays / 30);
            case QUARTERLY -> (int) (totalDays / 90);
            case ANNUAL -> (int) (totalDays / 365);
            case END_OF_TERM -> 1;
        };
    }
    
    private PaymentIncrement getPaymentIncrement(PaymentFrequency frequency) {
        return switch (frequency) {
            case HOURLY -> new PaymentIncrement(1, ChronoUnit.HOURS);
            case DAILY -> new PaymentIncrement(1, ChronoUnit.DAYS);
            case WEEKLY -> new PaymentIncrement(7, ChronoUnit.DAYS);
            case MONTHLY -> new PaymentIncrement(1, ChronoUnit.MONTHS);
            case QUARTERLY -> new PaymentIncrement(3, ChronoUnit.MONTHS);
            case ANNUAL -> new PaymentIncrement(1, ChronoUnit.YEARS);
            case END_OF_TERM -> new PaymentIncrement(1, ChronoUnit.YEARS); // Will be overridden
        };
    }
    
    private void submitForShariaApproval(IslamicFinancing financing, var complianceResult) {
        var complianceCheck = ComplianceCheck.builder()
            .financing(financing)
            .type(ComplianceType.SHARIA_SCREENING)
            .status(ComplianceStatus.UNDER_REVIEW)
            .scholarName("Sharia Board")
            .notes("Initial application screening")
            .checkedBy("System")
            .build();
        
        complianceCheckRepository.save(complianceCheck);
    }
    
    private void validatePayment(IslamicFinancing financing, PaymentRequest request) {
        if (financing.getStatus() != FinancingStatus.ACTIVE) {
            throw new PaymentException("Financing is not active");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Payment amount must be positive");
        }
    }
    
    private void updateFinancingAfterPayment(IslamicFinancing financing) {
        var remainingPayments = paymentScheduleRepository.countUnpaidPayments(financing.getId());
        
        if (remainingPayments == 0) {
            financing.setStatus(FinancingStatus.COMPLETED);
        } else {
            // Update next payment date
            var nextPayment = paymentScheduleRepository.findNextDuePayment(financing.getId());
            nextPayment.ifPresent(payment -> financing.setNextPaymentDate(payment.getDueDate()));
        }
        
        financingRepository.save(financing);
    }
    
    private FinancingResponse mapToResponse(IslamicFinancing financing) {
        return FinancingResponse.builder()
            .id(financing.getId())
            .financingReference(financing.getFinancingReference())
            .customerId(financing.getCustomerId())
            .financingType(financing.getFinancingType())
            .principalAmount(financing.getPrincipalAmount())
            .profitAmount(financing.getProfitAmount())
            .totalAmount(financing.getTotalAmount())
            .remainingAmount(financing.getRemainingAmount())
            .paymentFrequency(financing.getPaymentFrequency())
            .status(financing.getStatus())
            .startDate(financing.getStartDate())
            .maturityDate(financing.getMaturityDate())
            .nextPaymentDate(financing.getNextPaymentDate())
            .isAssetBacked(financing.getIsAssetBacked())
            .isShariaCompliant(financing.isShariaCompliant())
            .smartContractAddress(financing.getSmartContractAddress())
            .createdAt(financing.getCreatedAt())
            .build();
    }
    
    // Helper classes
    private record PaymentIncrement(long value, ChronoUnit unit) {}
}

// Custom Exceptions
class ShariaComplianceException extends RuntimeException {
    public ShariaComplianceException(String message) {
        super(message);
    }
}

class FinancingNotFoundException extends RuntimeException {
    public FinancingNotFoundException(String message) {
        super(message);
    }
}

class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}