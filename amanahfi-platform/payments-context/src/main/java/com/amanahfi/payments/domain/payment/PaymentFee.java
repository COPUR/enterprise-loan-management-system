package com.amanahfi.payments.domain.payment;

import com.amanahfi.shared.domain.money.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Fee Entity
 * Represents fees associated with a payment
 */
@Entity
@Table(name = "payment_fees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentFee {

    @Id
    private String feeId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentFeeType feeType;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "fee_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "fee_currency"))
    })
    private Money amount;

    @NotBlank
    private String description;

    @NotNull
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    public PaymentFee(PaymentFeeType feeType, Money amount, String description) {
        this.feeId = generateFeeId();
        this.feeType = feeType;
        this.amount = amount;
        this.description = description;
        this.appliedAt = LocalDateTime.now();
    }

    void assignToPayment(Payment payment) {
        this.payment = payment;
    }

    private String generateFeeId() {
        return "FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}