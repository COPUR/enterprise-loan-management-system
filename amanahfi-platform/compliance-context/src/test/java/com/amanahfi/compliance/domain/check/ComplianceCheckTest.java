package com.amanahfi.compliance.domain.check;

import com.amanahfi.shared.domain.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Compliance Check Aggregate
 * Following CBUAE AML regulations and Islamic finance compliance
 */
@DisplayName("Compliance Check Tests")
class ComplianceCheckTest {

    @Test
    @DisplayName("Should create AML check for customer onboarding")
    void shouldCreateAmlCheckForCustomerOnboarding() {
        // Given
        String customerId = "CUST-12345678";
        String checkReason = "New customer onboarding KYC verification";

        // When
        ComplianceCheck amlCheck = ComplianceCheck.createAmlCheck(
            customerId,
            CheckType.CUSTOMER_ONBOARDING,
            checkReason
        );

        // Then
        assertThat(amlCheck.getCheckId()).isNotNull();
        assertThat(amlCheck.getEntityId()).isEqualTo(customerId);
        assertThat(amlCheck.getCheckType()).isEqualTo(CheckType.CUSTOMER_ONBOARDING);
        assertThat(amlCheck.getComplianceType()).isEqualTo(ComplianceType.AML);
        assertThat(amlCheck.getReason()).isEqualTo(checkReason);
        assertThat(amlCheck.getStatus()).isEqualTo(CheckStatus.PENDING);
        assertThat(amlCheck.getCreatedAt()).isNotNull();
        assertThat(amlCheck.isAutomated()).isTrue(); // AML checks are typically automated
    }

    @Test
    @DisplayName("Should create Sharia compliance check for Murabaha contract")
    void shouldCreateShariahComplianceCheckForMurabahaContract() {
        // Given
        String contractId = "MUR-12345678";
        String checkReason = "Murabaha contract Sharia compliance verification";

        // When
        ComplianceCheck shariahCheck = ComplianceCheck.createShariahCheck(
            contractId,
            CheckType.CONTRACT_VALIDATION,
            checkReason
        );

        // Then
        assertThat(shariahCheck.getComplianceType()).isEqualTo(ComplianceType.SHARIA);
        assertThat(shariahCheck.getCheckType()).isEqualTo(CheckType.CONTRACT_VALIDATION);
        assertThat(shariahCheck.isAutomated()).isFalse(); // Sharia checks require human review
        assertThat(shariahCheck.requiresShariahBoardReview()).isTrue();
    }

    @Test
    @DisplayName("Should create transaction monitoring check")
    void shouldCreateTransactionMonitoringCheck() {
        // Given
        String transactionId = "PAY-12345678";
        Money amount = Money.of(new BigDecimal("50000.00"), "AED");
        String checkReason = "High-value transaction monitoring";

        // When
        ComplianceCheck transactionCheck = ComplianceCheck.createTransactionCheck(
            transactionId,
            amount,
            checkReason
        );

        // Then
        assertThat(transactionCheck.getCheckType()).isEqualTo(CheckType.TRANSACTION_MONITORING);
        assertThat(transactionCheck.getTransactionAmount()).isEqualTo(amount);
        assertThat(transactionCheck.isHighValueTransaction()).isTrue(); // Above 10K AED threshold
    }

    @ParameterizedTest
    @ValueSource(strings = {"10000.00", "25000.00", "100000.00"})
    @DisplayName("Should flag high-value transactions for enhanced monitoring")
    void shouldFlagHighValueTransactionsForEnhancedMonitoring(String amountStr) {
        // Given
        Money amount = Money.of(new BigDecimal(amountStr), "AED");
        
        // When
        ComplianceCheck check = ComplianceCheck.createTransactionCheck(
            "PAY-123", amount, "High value transaction"
        );

        // Then
        assertThat(check.isHighValueTransaction()).isTrue();
        assertThat(check.requiresEnhancedDueDiligence()).isTrue();
    }

    @Test
    @DisplayName("Should perform automated AML screening")
    void shouldPerformAutomatedAmlScreening() {
        // Given
        ComplianceCheck amlCheck = createAmlCheck();
        AmlScreeningResult screeningResult = new AmlScreeningResult(
            "AML-SCREEN-001",
            RiskScore.LOW,
            "No matches found in sanctions lists",
            List.of()
        );

        // When
        amlCheck.performAutomatedScreening(screeningResult);

        // Then
        assertThat(amlCheck.getStatus()).isEqualTo(CheckStatus.IN_PROGRESS);
        assertThat(amlCheck.getAmlScreeningResult()).isEqualTo(screeningResult);
        assertThat(amlCheck.getScreeningCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should complete check with low risk score")
    void shouldCompleteCheckWithLowRiskScore() {
        // Given
        ComplianceCheck check = createAmlCheck();
        check.performAutomatedScreening(createLowRiskScreeningResult());
        String completionNotes = "Automated screening passed - low risk customer";

        // When
        check.complete("SYSTEM", completionNotes, RiskScore.LOW);

        // Then
        assertThat(check.getStatus()).isEqualTo(CheckStatus.APPROVED);
        assertThat(check.getFinalRiskScore()).isEqualTo(RiskScore.LOW);
        assertThat(check.getCompletedAt()).isNotNull();
        assertThat(check.getCompletionNotes()).isEqualTo(completionNotes);
    }

    @Test
    @DisplayName("Should escalate high-risk cases for manual review")
    void shouldEscalateHighRiskCasesForManualReview() {
        // Given
        ComplianceCheck check = createAmlCheck();
        AmlScreeningResult highRiskResult = new AmlScreeningResult(
            "AML-SCREEN-002",
            RiskScore.HIGH,
            "Potential match found in sanctions list",
            List.of("Sanctions list match: 85% confidence")
        );

        // When
        check.performAutomatedScreening(highRiskResult);

        // Then
        assertThat(check.getStatus()).isEqualTo(CheckStatus.REQUIRES_REVIEW);
        assertThat(check.requiresManualReview()).isTrue();
        assertThat(check.getAmlScreeningResult().getRiskScore()).isEqualTo(RiskScore.HIGH);
    }

    @Test
    @DisplayName("Should assign check to compliance officer for manual review")
    void shouldAssignCheckToComplianceOfficerForManualReview() {
        // Given
        ComplianceCheck check = createHighRiskCheck();
        String officerId = "OFFICER-001";
        String assignmentNotes = "High-risk case requires detailed investigation";

        // When
        check.assignToOfficer(officerId, assignmentNotes);

        // Then
        assertThat(check.getStatus()).isEqualTo(CheckStatus.UNDER_INVESTIGATION);
        assertThat(check.getAssignedOfficerId()).isEqualTo(officerId);
        assertThat(check.getAssignmentNotes()).isEqualTo(assignmentNotes);
        assertThat(check.getAssignedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject non-compliant transaction")
    void shouldRejectNonCompliantTransaction() {
        // Given
        ComplianceCheck check = createTransactionCheck();
        String rejectionReason = "Transaction violates AML regulations - suspicious pattern detected";

        // When
        check.reject("OFFICER-001", rejectionReason);

        // Then
        assertThat(check.getStatus()).isEqualTo(CheckStatus.REJECTED);
        assertThat(check.getRejectionReason()).isEqualTo(rejectionReason);
        assertThat(check.getRejectedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should approve Sharia-compliant contract")
    void shouldApproveShariahCompliantContract() {
        // Given
        ComplianceCheck shariahCheck = createShariahCheck();
        String approvalNotes = "Contract structure complies with Islamic finance principles";
        String shariahBoardMember = "SCHOLAR-001";

        // When
        shariahCheck.assignToOfficer(shariahBoardMember, "Assigned for Sharia review");
        shariahCheck.complete(shariahBoardMember, approvalNotes, RiskScore.LOW);

        // Then
        assertThat(shariahCheck.getStatus()).isEqualTo(CheckStatus.APPROVED);
        assertThat(shariahCheck.isShariahCompliant()).isTrue();
    }

    @Test
    @DisplayName("Should track multiple compliance violations")
    void shouldTrackMultipleComplianceViolations() {
        // Given
        ComplianceCheck check = createAmlCheck();
        
        ComplianceViolation violation1 = new ComplianceViolation(
            ViolationType.SUSPICIOUS_TRANSACTION_PATTERN,
            "Multiple round-number transactions in short timeframe",
            SeverityLevel.MEDIUM
        );
        
        ComplianceViolation violation2 = new ComplianceViolation(
            ViolationType.SANCTIONS_LIST_MATCH,
            "Customer name matches entry in OFAC sanctions list",
            SeverityLevel.HIGH
        );

        // When
        check.addViolation(violation1);
        check.addViolation(violation2);

        // Then
        assertThat(check.getViolations()).hasSize(2);
        assertThat(check.getHighestSeverityLevel()).isEqualTo(SeverityLevel.HIGH);
        assertThat(check.hasViolations()).isTrue();
    }

    @Test
    @DisplayName("Should calculate compliance score based on violations")
    void shouldCalculateComplianceScoreBasedOnViolations() {
        // Given
        ComplianceCheck check = createAmlCheck();
        
        // Add violations of different severities
        check.addViolation(new ComplianceViolation(
            ViolationType.INCOMPLETE_DOCUMENTATION, "Missing ID copy", SeverityLevel.LOW
        ));
        check.addViolation(new ComplianceViolation(
            ViolationType.SUSPICIOUS_TRANSACTION_PATTERN, "Unusual activity", SeverityLevel.MEDIUM
        ));

        // When
        int complianceScore = check.calculateComplianceScore();

        // Then
        assertThat(complianceScore).isBetween(0, 100);
        assertThat(complianceScore).isLessThanOrEqualTo(80); // Should be 80 or lower due to violations
    }

    @Test
    @DisplayName("Should generate compliance report")
    void shouldGenerateComplianceReport() {
        // Given
        ComplianceCheck completedCheck = createCompletedCheck();

        // When
        ComplianceReport report = completedCheck.generateReport();

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getCheckId()).isEqualTo(completedCheck.getCheckId());
        assertThat(report.getComplianceScore()).isGreaterThan(0);
        assertThat(report.getRiskAssessment()).isNotBlank();
        assertThat(report.getRecommendations()).isNotEmpty();
    }

    @Test
    @DisplayName("Should enforce Islamic banking business rules")
    void shouldEnforceIslamicBankingBusinessRules() {
        // Given
        ComplianceCheck shariahCheck = createShariahCheck();

        // When & Then - Islamic finance compliance rules
        assertThat(shariahCheck.isShariahCompliant()).isTrue();
        assertThat(shariahCheck.allowsInterestBasedTransactions()).isFalse();
        assertThat(shariahCheck.requiresAssetBacking()).isTrue();
        assertThat(shariahCheck.prohibitsExcessiveUncertainty()).isTrue();
        assertThat(shariahCheck.requiresShariahBoardReview()).isTrue();
    }

    @Test
    @DisplayName("Should validate CBUAE regulatory requirements")
    void shouldValidateCbuaeRegulatoryRequirements() {
        // Given
        ComplianceCheck check = createAmlCheck();

        // When & Then - CBUAE compliance
        assertThat(check.meetsCbuaeRequirements()).isTrue();
        assertThat(check.hasRequiredDocumentation()).isTrue();
        assertThat(check.followsKycProcedures()).isTrue();
        assertThat(check.maintainsAuditTrail()).isTrue();
    }

    @Test
    @DisplayName("Should archive completed checks for audit purposes")
    void shouldArchiveCompletedChecksForAuditPurposes() {
        // Given
        ComplianceCheck completedCheck = createCompletedCheck();
        String archiveReason = "Routine archival after completion";

        // When
        completedCheck.archive(archiveReason);

        // Then
        assertThat(completedCheck.isArchived()).isTrue();
        assertThat(completedCheck.getArchivedAt()).isNotNull();
        assertThat(completedCheck.getArchiveReason()).isEqualTo(archiveReason);
    }

    // Helper methods
    private ComplianceCheck createAmlCheck() {
        return ComplianceCheck.createAmlCheck(
            "CUST-12345678",
            CheckType.CUSTOMER_ONBOARDING,
            "Standard KYC verification"
        );
    }

    private ComplianceCheck createShariahCheck() {
        return ComplianceCheck.createShariahCheck(
            "MUR-12345678",
            CheckType.CONTRACT_VALIDATION,
            "Murabaha contract compliance check"
        );
    }

    private ComplianceCheck createTransactionCheck() {
        return ComplianceCheck.createTransactionCheck(
            "PAY-12345678",
            Money.of(new BigDecimal("50000.00"), "AED"),
            "High-value transaction monitoring"
        );
    }

    private ComplianceCheck createHighRiskCheck() {
        ComplianceCheck check = createAmlCheck();
        AmlScreeningResult highRiskResult = new AmlScreeningResult(
            "AML-SCREEN-HIGH",
            RiskScore.HIGH,
            "High-risk indicators detected",
            List.of("Multiple risk factors")
        );
        check.performAutomatedScreening(highRiskResult);
        return check;
    }

    private ComplianceCheck createCompletedCheck() {
        ComplianceCheck check = createAmlCheck();
        check.performAutomatedScreening(createLowRiskScreeningResult());
        check.complete("SYSTEM", "Automated approval", RiskScore.LOW);
        return check;
    }

    private AmlScreeningResult createLowRiskScreeningResult() {
        return new AmlScreeningResult(
            "AML-SCREEN-LOW",
            RiskScore.LOW,
            "No adverse findings",
            List.of()
        );
    }
}