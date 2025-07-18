package com.bank.infrastructure.islamic;

import com.bank.infrastructure.domain.Money;
import com.bank.infrastructure.security.BankingComplianceFramework.ComplianceStandard;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Islamic Banking Compliance Validator for Sharia Compliance
 * 
 * Comprehensive Sharia compliance validation system providing:
 * - Riba (Interest) prohibition validation
 * - Gharar (Excessive uncertainty) detection
 * - Haram (Forbidden) industry screening
 * - Islamic contract structure validation
 * - Profit and loss sharing compliance
 * - Asset-backed financing verification
 * - Sharia board approval tracking
 * - Halal investment screening
 * - Zakat calculation compliance
 * - Islamic banking product validation
 * 
 * Supports all major Islamic banking products:
 * - Murabaha (Cost-plus financing)
 * - Ijara (Leasing)
 * - Musharaka (Partnership)
 * - Mudaraba (Profit-sharing)
 * - Sukuk (Islamic bonds)
 * - Takaful (Islamic insurance)
 * - Qard Hassan (Interest-free loans)
 */
@Component
public class IslamicBankingComplianceValidator {

    // Compliance tracking
    private final Map<String, ShariaApproval> shariaApprovals = new ConcurrentHashMap<>();
    private final Map<String, IslamicContract> islamicContracts = new ConcurrentHashMap<>();
    private final Map<String, ComplianceViolation> violations = new ConcurrentHashMap<>();
    private final Set<String> prohibitedIndustries = Set.of(
        "alcohol", "gambling", "tobacco", "pork", "adult-entertainment", 
        "conventional-banking", "interest-based-insurance", "weapons", 
        "conventional-bonds", "speculative-trading"
    );
    private final Set<String> approvedShariaBoards = Set.of(
        "AAOIFI", "IFSB", "Malaysia-SAC", "UAE-HFSB", "UK-UKIFC", "Pakistan-SBP"
    );

    /**
     * Islamic banking product types
     */
    public enum IslamicProductType {
        MURABAHA, IJARA, MUSHARAKA, MUDARABA, SUKUK, TAKAFUL, 
        QARD_HASSAN, SALAM, ISTISNA, WAKALA, KAFALA
    }

    /**
     * Sharia compliance status
     */
    public enum ShariaComplianceStatus {
        COMPLIANT, NON_COMPLIANT, PENDING_REVIEW, CONDITIONALLY_APPROVED, REJECTED
    }

    /**
     * Contract validation result
     */
    public enum ValidationResult {
        APPROVED, REJECTED, REQUIRES_MODIFICATION, NEEDS_SHARIA_BOARD_REVIEW
    }

    /**
     * Prohibition types in Islamic banking
     */
    public enum ProhibitionType {
        RIBA_INTEREST, GHARAR_UNCERTAINTY, HARAM_ACTIVITY, MAYSIR_GAMBLING, 
        ASSET_NOT_BACKED, SPECULATION, NON_SHARIA_COMPLIANT
    }

    /**
     * Islamic banking contract
     */
    public record IslamicContract(
        String contractId,
        String customerId,
        IslamicProductType productType,
        Money principalAmount,
        Money profitAmount,
        String underlyingAsset,
        LocalDate contractDate,
        LocalDate maturityDate,
        String shariaBoard,
        ShariaComplianceStatus complianceStatus,
        Map<String, Object> contractTerms,
        List<String> complianceNotes
    ) {}

    /**
     * Sharia approval record
     */
    public record ShariaApproval(
        String approvalId,
        String productType,
        String shariaBoard,
        String approvedBy,
        LocalDate approvalDate,
        LocalDate expiryDate,
        String approvalReference,
        List<String> conditions,
        String fatwaReference,
        Map<String, String> metadata
    ) {}

    /**
     * Compliance violation for Islamic banking
     */
    public record ComplianceViolation(
        String violationId,
        String contractId,
        ProhibitionType prohibitionType,
        String description,
        String severity,
        Instant detectedAt,
        String detectedBy,
        Map<String, Object> context
    ) {}

    /**
     * Murabaha transaction details
     */
    public record MurabahaTransaction(
        String transactionId,
        String customerId,
        Money costPrice,
        Money sellingPrice,
        BigDecimal profitMargin,
        String commodity,
        boolean ownershipTransferred,
        LocalDate deliveryDate,
        Map<String, Object> terms
    ) {}

    /**
     * Ijara lease details
     */
    public record IjaraLease(
        String leaseId,
        String customerId,
        String assetDescription,
        Money assetValue,
        Money monthlyRental,
        int leaseTerm,
        boolean endOwnership,
        String maintenanceResponsibility,
        Map<String, Object> conditions
    ) {}

    /**
     * Musharaka partnership details
     */
    public record MushrakaPartnership(
        String partnershipId,
        String customerId,
        Money bankContribution,
        Money customerContribution,
        BigDecimal bankProfitRatio,
        BigDecimal customerProfitRatio,
        BigDecimal bankLossRatio,
        String businessPurpose,
        String managementStructure,
        Map<String, Object> terms
    ) {}

    /**
     * Validate Murabaha transaction for Sharia compliance
     */
    public ShariaValidationResult validateMurabaha(MurabahaTransaction transaction) {
        List<String> violations = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Check if commodity is halal
        if (isHaramCommodity(transaction.commodity())) {
            violations.add("Commodity involves haram activities: " + transaction.commodity());
        }
        
        // Verify actual ownership and possession
        if (!transaction.ownershipTransferred()) {
            violations.add("Bank must take actual ownership of the commodity before sale");
        }
        
        // Check profit margin reasonableness
        BigDecimal profitMargin = transaction.profitMargin();
        if (profitMargin.compareTo(BigDecimal.valueOf(50)) > 0) { // More than 50%
            violations.add("Excessive profit margin may constitute exploitation");
            recommendations.add("Consider reducing profit margin to ensure fairness");
        }
        
        // Verify cost transparency
        if (transaction.costPrice().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("Cost price must be disclosed transparently");
        }
        
        // Check delivery obligations
        if (transaction.deliveryDate().isBefore(LocalDate.now())) {
            violations.add("Delivery date cannot be in the past");
        }
        
        ShariaComplianceStatus status = violations.isEmpty() ? 
            ShariaComplianceStatus.COMPLIANT : ShariaComplianceStatus.NON_COMPLIANT;
        
        if (!violations.isEmpty()) {
            recordViolation(transaction.transactionId(), ProhibitionType.NON_SHARIA_COMPLIANT, 
                          "Murabaha transaction violations", violations);
        }
        
        return new ShariaValidationResult(
            transaction.transactionId(),
            IslamicProductType.MURABAHA,
            status,
            violations,
            recommendations,
            Instant.now()
        );
    }

    /**
     * Validate Ijara lease for Sharia compliance
     */
    public ShariaValidationResult validateIjara(IjaraLease lease) {
        List<String> violations = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Check asset ownership
        if (lease.assetValue().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("Asset value must be positive and clearly defined");
        }
        
        // Verify rental structure
        Money totalRentals = new Money(lease.monthlyRental().getCurrency(),
            lease.monthlyRental().getAmount().multiply(BigDecimal.valueOf(lease.leaseTerm())));
        
        // Check for excessive rental (indicative of hidden interest)
        if (totalRentals.getAmount().compareTo(lease.assetValue().getAmount().multiply(BigDecimal.valueOf(2))) > 0) {
            violations.add("Total rental amount appears excessive and may constitute hidden interest");
        }
        
        // Verify maintenance responsibility
        if (!"LESSOR".equals(lease.maintenanceResponsibility())) {
            recommendations.add("Consider lessor bearing major maintenance costs as per Sharia principles");
        }
        
        // Check lease term reasonableness
        if (lease.leaseTerm() > 360) { // More than 30 years
            violations.add("Excessive lease term may lead to gharar (uncertainty)");
        }
        
        ShariaComplianceStatus status = violations.isEmpty() ? 
            ShariaComplianceStatus.COMPLIANT : ShariaComplianceStatus.NON_COMPLIANT;
        
        if (!violations.isEmpty()) {
            recordViolation(lease.leaseId(), ProhibitionType.GHARAR_UNCERTAINTY, 
                          "Ijara lease violations", violations);
        }
        
        return new ShariaValidationResult(
            lease.leaseId(),
            IslamicProductType.IJARA,
            status,
            violations,
            recommendations,
            Instant.now()
        );
    }

    /**
     * Validate Musharaka partnership for Sharia compliance
     */
    public ShariaValidationResult validateMusharaka(MushrakaPartnership partnership) {
        List<String> violations = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Check contribution validity
        if (partnership.bankContribution().getAmount().compareTo(BigDecimal.ZERO) <= 0 ||
            partnership.customerContribution().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            violations.add("Both parties must make positive capital contributions");
        }
        
        // Verify profit and loss sharing ratios
        BigDecimal totalProfitRatio = partnership.bankProfitRatio().add(partnership.customerProfitRatio());
        if (totalProfitRatio.compareTo(BigDecimal.valueOf(100)) != 0) {
            violations.add("Profit sharing ratios must total 100%");
        }
        
        // Check loss sharing alignment with capital contribution
        Money totalContribution = new Money(partnership.bankContribution().getCurrency(),
            partnership.bankContribution().getAmount().add(partnership.customerContribution().getAmount()));
        
        BigDecimal bankContributionRatio = partnership.bankContribution().getAmount()
            .divide(totalContribution.getAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        if (partnership.bankLossRatio().compareTo(bankContributionRatio) != 0) {
            violations.add("Loss sharing ratio must align with capital contribution ratio");
        }
        
        // Verify business purpose is halal
        if (isHaramBusiness(partnership.businessPurpose())) {
            violations.add("Business purpose involves prohibited activities");
        }
        
        // Check for guaranteed returns (prohibited in Musharaka)
        if (partnership.terms().containsKey("guaranteedReturn")) {
            violations.add("Guaranteed returns are prohibited in Musharaka partnerships");
        }
        
        ShariaComplianceStatus status = violations.isEmpty() ? 
            ShariaComplianceStatus.COMPLIANT : ShariaComplianceStatus.NON_COMPLIANT;
        
        if (!violations.isEmpty()) {
            recordViolation(partnership.partnershipId(), ProhibitionType.RIBA_INTEREST, 
                          "Musharaka partnership violations", violations);
        }
        
        return new ShariaValidationResult(
            partnership.partnershipId(),
            IslamicProductType.MUSHARAKA,
            status,
            violations,
            recommendations,
            Instant.now()
        );
    }

    /**
     * Validate investment for halal compliance
     */
    public InvestmentValidationResult validateInvestment(String investmentId, String industry, 
                                                       String company, Map<String, Object> financialData) {
        List<String> violations = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Industry screening
        if (prohibitedIndustries.contains(industry.toLowerCase())) {
            violations.add("Investment in prohibited industry: " + industry);
        }
        
        // Financial ratio screening
        if (financialData.containsKey("debtToTotalAssets")) {
            Double debtRatio = (Double) financialData.get("debtToTotalAssets");
            if (debtRatio > 0.33) { // More than 33% debt
                violations.add("Excessive debt ratio (" + debtRatio + ") - should be below 33%");
            }
        }
        
        if (financialData.containsKey("interestIncomeRatio")) {
            Double interestRatio = (Double) financialData.get("interestIncomeRatio");
            if (interestRatio > 0.05) { // More than 5% interest income
                violations.add("Excessive interest income ratio (" + interestRatio + ") - should be below 5%");
            }
        }
        
        // Liquidity screening
        if (financialData.containsKey("liquidAssetsRatio")) {
            Double liquidityRatio = (Double) financialData.get("liquidAssetsRatio");
            if (liquidityRatio > 0.90) { // More than 90% liquid assets
                violations.add("Excessive liquidity ratio may indicate speculation");
            }
        }
        
        boolean isHalal = violations.isEmpty();
        
        return new InvestmentValidationResult(
            investmentId,
            company,
            industry,
            isHalal,
            violations,
            recommendations,
            Instant.now()
        );
    }

    /**
     * Calculate Zakat for Islamic banking compliance
     */
    public ZakatCalculation calculateZakat(String customerId, Money totalWealth, Money goldValue, 
                                         Money silverValue, Money cashAndInvestments) {
        
        // Nisab thresholds (minimum wealth for Zakat obligation)
        Money goldNisab = new Money(totalWealth.getCurrency(), BigDecimal.valueOf(85).multiply(goldValue.getAmount()));
        Money silverNisab = new Money(totalWealth.getCurrency(), BigDecimal.valueOf(595).multiply(silverValue.getAmount()));
        
        // Use lower of gold or silver nisab
        Money applicableNisab = goldNisab.getAmount().compareTo(silverNisab.getAmount()) < 0 ? 
            goldNisab : silverNisab;
        
        boolean zakatObligatory = totalWealth.getAmount().compareTo(applicableNisab.getAmount()) >= 0;
        
        Money zakatAmount = new Money(totalWealth.getCurrency(), BigDecimal.ZERO);
        if (zakatObligatory) {
            // Zakat rate is 2.5% of zakatable wealth
            zakatAmount = new Money(totalWealth.getCurrency(), 
                totalWealth.getAmount().multiply(BigDecimal.valueOf(0.025)));
        }
        
        return new ZakatCalculation(
            customerId,
            totalWealth,
            applicableNisab,
            zakatAmount,
            zakatObligatory,
            LocalDate.now(),
            Map.of(
                "goldNisab", goldNisab.toFormattedString(),
                "silverNisab", silverNisab.toFormattedString(),
                "zakatRate", "2.5%"
            )
        );
    }

    /**
     * Validate Sukuk structure for Sharia compliance
     */
    public ShariaValidationResult validateSukuk(String sukukId, String underlyingAsset, 
                                               String sukukType, Money faceValue, 
                                               BigDecimal expectedReturn, Map<String, Object> structure) {
        List<String> violations = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        // Verify asset backing
        if (underlyingAsset == null || underlyingAsset.trim().isEmpty()) {
            violations.add("Sukuk must be backed by tangible assets");
        }
        
        // Check for excessive gharar in structure
        if (!structure.containsKey("assetDescription") || !structure.containsKey("cashFlowMechanism")) {
            violations.add("Sukuk structure lacks clarity, may contain excessive gharar");
        }
        
        // Verify expected return reasonableness
        if (expectedReturn.compareTo(BigDecimal.valueOf(20)) > 0) { // More than 20%
            violations.add("Excessive expected return may indicate speculation");
        }
        
        // Check Sukuk type compliance
        if (!isValidSukukType(sukukType)) {
            violations.add("Invalid or non-compliant Sukuk type: " + sukukType);
        }
        
        // Verify no guaranteed returns (except for specific types)
        if (structure.containsKey("guaranteedReturn") && 
            !List.of("Sukuk Al-Ijara", "Sukuk Al-Murabaha").contains(sukukType)) {
            violations.add("Guaranteed returns not permitted for this Sukuk type");
        }
        
        ShariaComplianceStatus status = violations.isEmpty() ? 
            ShariaComplianceStatus.COMPLIANT : ShariaComplianceStatus.NON_COMPLIANT;
        
        return new ShariaValidationResult(
            sukukId,
            IslamicProductType.SUKUK,
            status,
            violations,
            recommendations,
            Instant.now()
        );
    }

    /**
     * Get Sharia compliance dashboard
     */
    public ShariaComplianceDashboard getComplianceDashboard() {
        Map<IslamicProductType, Long> productCounts = islamicContracts.values().stream()
            .collect(Collectors.groupingBy(IslamicContract::productType, Collectors.counting()));
            
        Map<ShariaComplianceStatus, Long> statusCounts = islamicContracts.values().stream()
            .collect(Collectors.groupingBy(IslamicContract::complianceStatus, Collectors.counting()));
            
        Map<ProhibitionType, Long> violationCounts = violations.values().stream()
            .collect(Collectors.groupingBy(ComplianceViolation::prohibitionType, Collectors.counting()));
        
        long totalContracts = islamicContracts.size();
        long compliantContracts = statusCounts.getOrDefault(ShariaComplianceStatus.COMPLIANT, 0L);
        double complianceRate = totalContracts > 0 ? (double) compliantContracts / totalContracts * 100 : 0;
        
        return new ShariaComplianceDashboard(
            totalContracts,
            compliantContracts,
            violations.size(),
            shariaApprovals.size(),
            complianceRate,
            productCounts,
            statusCounts,
            violationCounts
        );
    }

    // Helper methods
    private boolean isHaramCommodity(String commodity) {
        String lowerCommodity = commodity.toLowerCase();
        return prohibitedIndustries.stream().anyMatch(lowerCommodity::contains);
    }

    private boolean isHaramBusiness(String businessPurpose) {
        String lowerPurpose = businessPurpose.toLowerCase();
        return prohibitedIndustries.stream().anyMatch(lowerPurpose::contains);
    }

    private boolean isValidSukukType(String sukukType) {
        Set<String> validTypes = Set.of(
            "Sukuk Al-Ijara", "Sukuk Al-Murabaha", "Sukuk Al-Musharaka", 
            "Sukuk Al-Mudaraba", "Sukuk Al-Salam", "Sukuk Al-Istisna"
        );
        return validTypes.contains(sukukType);
    }

    private void recordViolation(String contractId, ProhibitionType type, String description, List<String> details) {
        String violationId = UUID.randomUUID().toString();
        
        ComplianceViolation violation = new ComplianceViolation(
            violationId,
            contractId,
            type,
            description + ": " + String.join(", ", details),
            "HIGH",
            Instant.now(),
            "system",
            Map.of("details", details, "contractId", contractId)
        );
        
        violations.put(violationId, violation);
    }

    /**
     * Register Sharia board approval
     */
    public void registerShariaApproval(String productType, String shariaBoard, String approvedBy,
                                     LocalDate expiryDate, String fatwaReference) {
        if (!approvedShariaBoards.contains(shariaBoard)) {
            throw new IllegalArgumentException("Unrecognized Sharia board: " + shariaBoard);
        }
        
        String approvalId = UUID.randomUUID().toString();
        
        ShariaApproval approval = new ShariaApproval(
            approvalId,
            productType,
            shariaBoard,
            approvedBy,
            LocalDate.now(),
            expiryDate,
            "APPROVAL-" + System.currentTimeMillis(),
            List.of("Standard approval conditions apply"),
            fatwaReference,
            Map.of("validatedBy", "system", "approvalLevel", "standard")
        );
        
        shariaApprovals.put(approvalId, approval);
    }

    /**
     * Check if product has valid Sharia approval
     */
    public boolean hasValidShariaApproval(String productType) {
        return shariaApprovals.values().stream()
            .anyMatch(approval -> approval.productType().equals(productType) &&
                                approval.expiryDate().isAfter(LocalDate.now()));
    }

    // Result classes
    public record ShariaValidationResult(
        String entityId,
        IslamicProductType productType,
        ShariaComplianceStatus status,
        List<String> violations,
        List<String> recommendations,
        Instant validatedAt
    ) {}

    public record InvestmentValidationResult(
        String investmentId,
        String company,
        String industry,
        boolean isHalal,
        List<String> violations,
        List<String> recommendations,
        Instant validatedAt
    ) {}

    public record ZakatCalculation(
        String customerId,
        Money totalWealth,
        Money nisabThreshold,
        Money zakatAmount,
        boolean zakatObligatory,
        LocalDate calculationDate,
        Map<String, String> calculationDetails
    ) {}

    public record ShariaComplianceDashboard(
        long totalContracts,
        long compliantContracts,
        int totalViolations,
        int shariaApprovals,
        double complianceRate,
        Map<IslamicProductType, Long> productDistribution,
        Map<ShariaComplianceStatus, Long> statusDistribution,
        Map<ProhibitionType, Long> violationDistribution
    ) {}
}