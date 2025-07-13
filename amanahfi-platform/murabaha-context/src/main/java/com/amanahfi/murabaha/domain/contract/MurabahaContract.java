package com.amanahfi.murabaha.domain.contract;

import com.amanahfi.shared.domain.money.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Murabaha Contract Aggregate Root
 * 
 * Represents an Islamic finance Murabaha contract following Sharia principles:
 * - Asset-backed financing (no speculation)
 * - Transparent profit margin (no hidden interest)
 * - Actual ownership transfer
 * - Compliant with CBUAE and HSA guidelines
 */
@Entity
@Table(name = "murabaha_contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MurabahaContract {

    // Business Constants
    private static final BigDecimal MINIMUM_CONTRACT_AMOUNT = new BigDecimal("10000.00"); // 10K AED minimum
    private static final BigDecimal MAXIMUM_PROFIT_RATE = new BigDecimal("0.25"); // 25% maximum profit rate
    private static final int MAXIMUM_TERM_MONTHS = 84; // 7 years maximum
    private static final int MINIMUM_TERM_MONTHS = 6; // 6 months minimum
    
    @Id
    private String contractId;

    @NotBlank
    private String customerId;

    @NotBlank
    private String assetDescription;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "asset_cost_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "asset_cost_currency"))
    })
    private Money assetCost;

    @NotNull
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "profit_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "profit_currency"))
    })
    private Money profitAmount;

    @NotNull
    private Integer termMonths;

    @NotNull
    private LocalDate deliveryDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @NotNull
    private LocalDateTime createdAt;

    // Sharia Compliance
    private LocalDateTime shariahApprovalDate;
    private String shariahBoardMemberId;
    private String shariahApprovalNotes;

    // Activation
    private LocalDateTime activationDate;
    private String activationReference;

    // Asset Delivery
    private boolean assetDelivered = false;
    private LocalDate assetDeliveryDate;
    private String deliveryReference;
    private String deliveryNotes;

    // Settlement
    private LocalDate settlementDate;
    private String settlementReference;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "early_settlement_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "early_settlement_currency"))
    })
    private Money earlySettlementAmount;

    // Default Handling
    private LocalDate defaultDate;
    private String defaultReason;

    // Installment Schedule
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InstallmentSchedule> installmentSchedule = new ArrayList<>();

    // Domain Events
    @Transient
    private List<Object> domainEvents = new ArrayList<>();

    /**
     * Creates a new Murabaha contract
     */
    public static MurabahaContract create(String customerId, String assetDescription, 
                                        Money assetCost, Money profitAmount, 
                                        int termMonths, LocalDate deliveryDate) {
        validateContractParameters(customerId, assetDescription, assetCost, profitAmount, termMonths, deliveryDate);
        validateBusinessRules(assetCost, profitAmount, termMonths);
        
        MurabahaContract contract = new MurabahaContract();
        contract.contractId = generateContractId();
        contract.customerId = customerId;
        contract.assetDescription = assetDescription.trim();
        contract.assetCost = assetCost;
        contract.profitAmount = profitAmount;
        contract.termMonths = termMonths;
        contract.deliveryDate = deliveryDate;
        contract.status = ContractStatus.DRAFT;
        contract.createdAt = LocalDateTime.now();
        
        contract.addDomainEvent(new MurabahaContractCreatedEvent(contract.contractId, customerId, assetCost, profitAmount));
        
        return contract;
    }

    /**
     * Approves contract by Sharia Supervisory Board
     */
    public void approveByShariahBoard(String boardMemberId, String approvalNotes) {
        if (status != ContractStatus.DRAFT) {
            throw new IllegalStateException("Only draft contracts can be approved by Sharia board");
        }
        
        this.status = ContractStatus.SHARIA_APPROVED;
        this.shariahApprovalDate = LocalDateTime.now();
        this.shariahBoardMemberId = boardMemberId;
        this.shariahApprovalNotes = approvalNotes;
        
        addDomainEvent(new ContractShariahApprovedEvent(contractId, boardMemberId));
    }

    /**
     * Activates the contract and generates installment schedule
     */
    public void activate(String activationReference) {
        if (status != ContractStatus.SHARIA_APPROVED) {
            throw new IllegalStateException("Contract must be Sharia approved before activation");
        }
        
        this.status = ContractStatus.ACTIVE;
        this.activationDate = LocalDateTime.now();
        this.activationReference = activationReference;
        
        generateInstallmentSchedule();
        
        addDomainEvent(new ContractActivatedEvent(contractId, activationReference));
    }

    /**
     * Records installment payment
     */
    public void recordInstallmentPayment(int installmentNumber, Money paymentAmount, String paymentReference) {
        if (status != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Contract must be active to record payments");
        }
        
        InstallmentSchedule installment = findInstallmentByNumber(installmentNumber);
        if (installment == null) {
            throw new IllegalArgumentException("Installment number not found: " + installmentNumber);
        }
        
        installment.markAsPaid(paymentAmount, paymentReference);
        
        // Check if contract is fully paid
        if (isFullyPaid()) {
            this.status = ContractStatus.COMPLETED;
        }
        
        addDomainEvent(new InstallmentPaidEvent(contractId, installmentNumber, paymentAmount));
    }

    /**
     * Settles contract early with discount
     */
    public void settleEarly(Money settlementAmount, String settlementReference) {
        if (status != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Only active contracts can be settled early");
        }
        
        this.status = ContractStatus.SETTLED;
        this.settlementDate = LocalDate.now();
        this.settlementReference = settlementReference;
        this.earlySettlementAmount = settlementAmount;
        
        addDomainEvent(new ContractEarlySettledEvent(contractId, settlementAmount));
    }

    /**
     * Marks contract as defaulted
     */
    public void markAsDefault(String reason) {
        if (status != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Only active contracts can be marked as default");
        }
        
        this.status = ContractStatus.DEFAULTED;
        this.defaultDate = LocalDate.now();
        this.defaultReason = reason;
        
        addDomainEvent(new ContractDefaultedEvent(contractId, reason));
    }

    /**
     * Confirms asset delivery to customer
     */
    public void confirmAssetDelivery(String deliveryReference, String deliveryNotes) {
        this.assetDelivered = true;
        this.assetDeliveryDate = LocalDate.now();
        this.deliveryReference = deliveryReference;
        this.deliveryNotes = deliveryNotes;
        
        addDomainEvent(new AssetDeliveredEvent(contractId, deliveryReference));
    }

    // Business Logic Methods

    public Money getTotalSellingPrice() {
        return assetCost.add(profitAmount);
    }

    public Money getMonthlyInstallmentAmount() {
        Money totalAmount = getTotalSellingPrice();
        BigDecimal monthlyAmount = totalAmount.getAmount().divide(
            new BigDecimal(termMonths), 2, RoundingMode.HALF_UP
        );
        return Money.of(monthlyAmount, totalAmount.getCurrency());
    }

    public BigDecimal getProfitRate() {
        return profitAmount.getAmount().divide(assetCost.getAmount(), 4, RoundingMode.HALF_UP);
    }

    public boolean isProfitRateWithinLimits() {
        return getProfitRate().compareTo(MAXIMUM_PROFIT_RATE) <= 0;
    }

    public boolean isTermWithinLimits() {
        return termMonths >= MINIMUM_TERM_MONTHS && termMonths <= MAXIMUM_TERM_MONTHS;
    }

    public boolean isShariahCompliant() {
        return true; // Murabaha is inherently Sharia compliant
    }

    public boolean hasInterestBasedCharges() {
        return false; // Murabaha uses profit, not interest
    }

    public boolean hasAssetBacking() {
        return assetDescription != null && !assetDescription.trim().isEmpty();
    }

    public boolean allowsSpeculation() {
        return false; // Islamic finance prohibits speculation
    }

    public boolean requiresActualOwnership() {
        return true; // Bank must own asset before selling
    }

    public boolean isAssetDelivered() {
        return assetDelivered;
    }

    public Money getOutstandingBalance() {
        Money totalPaid = Money.zero(assetCost.getCurrency());
        
        for (InstallmentSchedule installment : installmentSchedule) {
            if (installment.getStatus() == InstallmentStatus.PAID && installment.getPaidAmount() != null) {
                totalPaid = totalPaid.add(installment.getPaidAmount());
            }
        }
        
        return getTotalSellingPrice().subtract(totalPaid);
    }

    private boolean isFullyPaid() {
        return installmentSchedule.stream()
            .allMatch(installment -> installment.getStatus() == InstallmentStatus.PAID);
    }

    private InstallmentSchedule findInstallmentByNumber(int installmentNumber) {
        return installmentSchedule.stream()
            .filter(installment -> installment.getInstallmentNumber().equals(installmentNumber))
            .findFirst()
            .orElse(null);
    }

    private void generateInstallmentSchedule() {
        Money monthlyAmount = getMonthlyInstallmentAmount();
        LocalDate currentDueDate = deliveryDate.plusMonths(1); // First payment after delivery
        
        for (int i = 1; i <= termMonths; i++) {
            InstallmentSchedule installment = new InstallmentSchedule(i, monthlyAmount, currentDueDate);
            installment.assignToContract(this);
            this.installmentSchedule.add(installment);
            currentDueDate = currentDueDate.plusMonths(1);
        }
    }

    // Validation Methods

    private static void validateContractParameters(String customerId, String assetDescription, 
                                                 Money assetCost, Money profitAmount, 
                                                 int termMonths, LocalDate deliveryDate) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (assetDescription == null || assetDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset description cannot be empty - Murabaha requires asset backing");
        }
        if (assetCost == null || !assetCost.isPositive()) {
            throw new IllegalArgumentException("Asset cost must be positive");
        }
        if (profitAmount == null || !profitAmount.isPositive()) {
            throw new IllegalArgumentException("Profit amount must be positive");
        }
        if (!assetCost.getCurrency().equals(profitAmount.getCurrency())) {
            throw new IllegalArgumentException("Asset cost and profit amount must have same currency");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term months must be positive");
        }
        if (deliveryDate == null || deliveryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Delivery date must be in the future");
        }
    }

    private static void validateBusinessRules(Money assetCost, Money profitAmount, int termMonths) {
        // Validate minimum contract amount
        if (assetCost.getAmount().compareTo(MINIMUM_CONTRACT_AMOUNT) < 0) {
            throw new InvalidContractAmountException(
                String.format("Contract amount %s below minimum %s %s", 
                    assetCost.getAmount(), MINIMUM_CONTRACT_AMOUNT, assetCost.getCurrency())
            );
        }
        
        // Validate profit rate
        BigDecimal profitRate = profitAmount.getAmount().divide(assetCost.getAmount(), 4, RoundingMode.HALF_UP);
        if (profitRate.compareTo(MAXIMUM_PROFIT_RATE) > 0) {
            throw new ExcessiveProfitRateException(
                String.format("Profit rate %.2f%% exceeds maximum allowed %.2f%%", 
                    profitRate.multiply(new BigDecimal("100")), 
                    MAXIMUM_PROFIT_RATE.multiply(new BigDecimal("100")))
            );
        }
        
        // Validate term
        if (termMonths < MINIMUM_TERM_MONTHS || termMonths > MAXIMUM_TERM_MONTHS) {
            throw new IllegalArgumentException(
                String.format("Term %d months outside allowed range %d-%d months", 
                    termMonths, MINIMUM_TERM_MONTHS, MAXIMUM_TERM_MONTHS)
            );
        }
    }

    private static String generateContractId() {
        return "MUR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}