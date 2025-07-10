package com.masrufi.sharia.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Schedule Entity
 * Manages individual payment installments for Islamic financing
 */
@Entity
@Table(name = "payment_schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financing_id", nullable = false)
    private IslamicFinancing financing;
    
    @Column(nullable = false)
    private Integer installmentNumber;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private LocalDateTime dueDate;
    
    @Column
    private LocalDateTime paidDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPaid = false;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal paidAmount;
    
    @Column
    private String transactionHash;
    
    @Column
    private String paymentMethod; // BTC, ETH, USDT, etc.
    
    @Column(length = 500)
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Business Methods
    
    /**
     * Mark payment as completed
     */
    public void markAsPaid(BigDecimal paidAmount, String transactionHash) {
        this.isPaid = true;
        this.paidAmount = paidAmount;
        this.paidDate = LocalDateTime.now();
        this.transactionHash = transactionHash;
        this.status = PaymentStatus.COMPLETED;
    }
    
    /**
     * Check if payment is overdue
     */
    public boolean isOverdue() {
        return !this.isPaid && this.dueDate.isBefore(LocalDateTime.now());
    }
    
    /**
     * Get days until due date
     */
    public long getDaysUntilDue() {
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(), this.dueDate
        );
    }
    
    /**
     * Check if payment is due soon (within 3 days)
     */
    public boolean isDueSoon() {
        long daysUntilDue = getDaysUntilDue();
        return !this.isPaid && daysUntilDue >= 0 && daysUntilDue <= 3;
    }
}

enum PaymentStatus {
    PENDING("Pending Payment"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    CANCELLED("Cancelled");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}