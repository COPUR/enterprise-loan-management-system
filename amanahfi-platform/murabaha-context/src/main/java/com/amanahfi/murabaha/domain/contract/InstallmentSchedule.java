package com.amanahfi.murabaha.domain.contract;

import com.amanahfi.shared.domain.money.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Installment Schedule Entity
 * Represents individual installment within a Murabaha contract
 */
@Entity
@Table(name = "installment_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstallmentSchedule {

    @Id
    private String installmentId;

    @NotNull
    private Integer installmentNumber;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "installment_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "installment_currency"))
    })
    private Money amount;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private InstallmentStatus status;

    private LocalDate paidDate;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "paid_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "paid_currency"))
    })
    private Money paidAmount;

    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private MurabahaContract contract;

    public InstallmentSchedule(Integer installmentNumber, Money amount, LocalDate dueDate) {
        this.installmentId = generateInstallmentId();
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = InstallmentStatus.PENDING;
    }

    void assignToContract(MurabahaContract contract) {
        this.contract = contract;
    }

    public void markAsPaid(Money paidAmount, String paymentReference) {
        this.status = InstallmentStatus.PAID;
        this.paidAmount = paidAmount;
        this.paymentReference = paymentReference;
        this.paidDate = LocalDate.now();
    }

    public boolean isOverdue() {
        return status == InstallmentStatus.PENDING && dueDate.isBefore(LocalDate.now());
    }

    private String generateInstallmentId() {
        return "INST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}