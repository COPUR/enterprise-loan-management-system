package com.loanmanagement.ai.service;

import com.loanmanagement.ai.domain.model.*;
import com.loanmanagement.ai.application.port.in.*;
import com.loanmanagement.ai.application.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AI Loan Analysis Service Test
 * Tests for AI-powered loan analysis capabilities
 */
@SpringBootTest
class AILoanAnalysisServiceTest {

    private AILoanAnalysisService aiAnalysisService;
    private LoanRiskAssessmentService riskAssessmentService;
    private CreditScoringService creditScoringService;
    private DocumentAnalysisService documentAnalysisService;
    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        // These services don't exist yet - tests will fail initially
        aiAnalysisService = new AILoanAnalysisService();
        riskAssessmentService = new LoanRiskAssessmentService();
        creditScoringService = new CreditScoringService();
        documentAnalysisService = new DocumentAnalysisService();
        fraudDetectionService = new FraudDetectionService();
    }

    @Test
    @DisplayName("Should analyze loan application using AI")
    void shouldAnalyzeLoanApplicationUsingAI() {
        // Create loan analysis request
        LoanAnalysisRequest request = LoanAnalysisRequest.builder()
            .customerId("CUST-001")
            .requestedAmount(BigDecimal.valueOf(50000))
            .loanPurpose(LoanPurpose.HOME_PURCHASE)
            .annualIncome(BigDecimal.valueOf(75000))
            .employmentType(EmploymentType.FULL_TIME)
            .creditScore(720)
            .debtToIncomeRatio(0.35)
            .build();

        // Perform AI analysis
        LoanAnalysisResult result = aiAnalysisService.analyzeLoanApplication(request);

        // Verify analysis results
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo(request.getRequestId());
        assertThat(result.getRiskScore()).isBetween(0.0, 1.0);
        assertThat(result.getRecommendation()).isIn(LoanRecommendation.APPROVE, LoanRecommendation.CONDITIONAL_APPROVE, LoanRecommendation.REJECT);
        assertThat(result.getConfidenceLevel()).isBetween(0.0, 1.0);
        assertThat(result.getRiskFactors()).isNotEmpty();
    }

    @Test
    @DisplayName("Should perform comprehensive risk assessment")
    void shouldPerformComprehensiveRiskAssessment() {
        // Create customer profile
        CustomerFinancialProfile profile = CustomerFinancialProfile.builder()
            .customerId("CUST-002")
            .annualIncome(BigDecimal.valueOf(60000))
            .monthlyExpenses(BigDecimal.valueOf(3500))
            .existingDebt(BigDecimal.valueOf(15000))
            .creditScore(680)
            .employmentHistory(36) // 3 years
            .bankingHistory(60) // 5 years
            .build();

        // Perform risk assessment
        RiskAssessmentResult result = riskAssessmentService.assessRisk(profile);

        // Verify risk assessment
        assertThat(result).isNotNull();
        assertThat(result.getOverallRiskLevel()).isIn(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH);
        assertThat(result.getRiskScore()).isBetween(0.0, 100.0);
        assertThat(result.getRiskFactors()).isNotEmpty();
        assertThat(result.getMitigatingFactors()).isNotNull();
        assertThat(result.getRecommendedInterestRate()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should generate dynamic credit score using ML")
    void shouldGenerateDynamicCreditScoreUsingML() {
        // Create credit analysis request
        CreditAnalysisRequest request = CreditAnalysisRequest.builder()
            .customerId("CUST-003")
            .staticCreditScore(650)
            .paymentHistory(createPaymentHistory())
            .accountAges(List.of(24, 36, 48)) // months
            .creditUtilization(0.45)
            .recentInquiries(2)
            .accountTypes(List.of("credit_card", "auto_loan", "mortgage"))
            .build();

        // Generate dynamic credit score
        CreditScoreResult result = creditScoringService.generateDynamicCreditScore(request);

        // Verify credit score result
        assertThat(result).isNotNull();
        assertThat(result.getDynamicScore()).isBetween(300, 850);
        assertThat(result.getScoreChange()).isNotNull();
        assertThat(result.getContributingFactors()).isNotEmpty();
        assertThat(result.getRecommendations()).isNotEmpty();
        assertThat(result.getConfidenceInterval()).isNotNull();
    }

    @Test
    @DisplayName("Should analyze uploaded documents using AI")
    void shouldAnalyzeUploadedDocumentsUsingAI() {
        // Create document analysis request
        DocumentAnalysisRequest request = DocumentAnalysisRequest.builder()
            .documentId("DOC-001")
            .documentType(DocumentType.PAY_STUB)
            .documentContent("base64-encoded-document-content")
            .customerId("CUST-004")
            .build();

        // Analyze document
        DocumentAnalysisResult result = documentAnalysisService.analyzeDocument(request);

        // Verify document analysis
        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo("DOC-001");
        assertThat(result.isValid()).isNotNull();
        assertThat(result.getExtractedData()).isNotEmpty();
        assertThat(result.getConfidenceScore()).isBetween(0.0, 1.0);
        assertThat(result.getValidationErrors()).isNotNull();
        assertThat(result.getSuggestedCorrections()).isNotNull();
    }

    @Test
    @DisplayName("Should detect fraud patterns using ML")
    void shouldDetectFraudPatternsUsingML() {
        // Create fraud detection request
        FraudDetectionRequest request = FraudDetectionRequest.builder()
            .customerId("CUST-005")
            .applicationData(createApplicationData())
            .deviceFingerprint("device-fingerprint-123")
            .ipAddress("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .applicationTime(LocalDateTime.now())
            .build();

        // Perform fraud detection
        FraudDetectionResult result = fraudDetectionService.detectFraud(request);

        // Verify fraud detection results
        assertThat(result).isNotNull();
        assertThat(result.getFraudScore()).isBetween(0.0, 1.0);
        assertThat(result.getRiskLevel()).isIn(FraudRiskLevel.LOW, FraudRiskLevel.MEDIUM, FraudRiskLevel.HIGH, FraudRiskLevel.CRITICAL);
        assertThat(result.getDetectedPatterns()).isNotNull();
        assertThat(result.getFraudIndicators()).isNotNull();
        assertThat(result.getRecommendedAction()).isNotNull();
    }

    @Test
    @DisplayName("Should provide loan recommendations based on AI analysis")
    void shouldProvideLoanRecommendationsBasedOnAIAnalysis() {
        // Create recommendation request
        LoanRecommendationRequest request = LoanRecommendationRequest.builder()
            .customerId("CUST-006")
            .customerProfile(createCustomerProfile())
            .requestedAmount(BigDecimal.valueOf(100000))
            .preferredTerm(360) // 30 years
            .loanPurpose(LoanPurpose.HOME_PURCHASE)
            .build();

        // Generate recommendations
        LoanRecommendationResult result = aiAnalysisService.generateLoanRecommendations(request);

        // Verify recommendations
        assertThat(result).isNotNull();
        assertThat(result.getRecommendations()).isNotEmpty();
        assertThat(result.getRecommendations()).hasSize(3); // Top 3 recommendations
        
        LoanRecommendation topRecommendation = result.getRecommendations().get(0);
        assertThat(topRecommendation.getLoanType()).isNotNull();
        assertThat(topRecommendation.getInterestRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(topRecommendation.getTermMonths()).isGreaterThan(0);
        assertThat(topRecommendation.getMonthlyPayment()).isGreaterThan(BigDecimal.ZERO);
        assertThat(topRecommendation.getApprovalProbability()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should handle batch loan analysis efficiently")
    void shouldHandleBatchLoanAnalysisEfficiently() {
        // Create batch analysis request
        List<LoanAnalysisRequest> batchRequests = List.of(
            createLoanAnalysisRequest("CUST-007", BigDecimal.valueOf(25000)),
            createLoanAnalysisRequest("CUST-008", BigDecimal.valueOf(45000)),
            createLoanAnalysisRequest("CUST-009", BigDecimal.valueOf(75000))
        );

        // Perform batch analysis
        BatchAnalysisResult result = aiAnalysisService.analyzeBatch(batchRequests);

        // Verify batch results
        assertThat(result).isNotNull();
        assertThat(result.getResults()).hasSize(3);
        assertThat(result.getProcessingTimeMs()).isGreaterThan(0L);
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should provide explainable AI decision rationale")
    void shouldProvideExplainableAIDecisionRationale() {
        // Create analysis request
        LoanAnalysisRequest request = createLoanAnalysisRequest("CUST-010", BigDecimal.valueOf(80000));

        // Perform analysis with explanation
        LoanAnalysisResult result = aiAnalysisService.analyzeLoanApplication(request);
        ExplanationResult explanation = aiAnalysisService.explainDecision(result.getAnalysisId());

        // Verify explanation
        assertThat(explanation).isNotNull();
        assertThat(explanation.getDecisionFactors()).isNotEmpty();
        assertThat(explanation.getFeatureImportance()).isNotEmpty();
        assertThat(explanation.getWhatIfScenarios()).isNotEmpty();
        assertThat(explanation.getRegulatorySummary()).isNotNull();
    }

    @Test
    @DisplayName("Should validate input data before analysis")
    void shouldValidateInputDataBeforeAnalysis() {
        // Create invalid request (negative amount)
        LoanAnalysisRequest invalidRequest = LoanAnalysisRequest.builder()
            .customerId("CUST-011")
            .requestedAmount(BigDecimal.valueOf(-1000)) // Invalid negative amount
            .loanPurpose(LoanPurpose.PERSONAL)
            .build();

        // Should throw validation exception
        assertThrows(InvalidLoanAnalysisRequestException.class, () -> {
            aiAnalysisService.analyzeLoanApplication(invalidRequest);
        });
    }

    @Test
    @DisplayName("Should handle ML model unavailability gracefully")
    void shouldHandleMLModelUnavailabilityGracefully() {
        // Simulate model unavailability
        aiAnalysisService.setModelAvailable(false);
        
        LoanAnalysisRequest request = createLoanAnalysisRequest("CUST-012", BigDecimal.valueOf(30000));

        // Should fall back to rule-based analysis
        LoanAnalysisResult result = aiAnalysisService.analyzeLoanApplication(request);

        assertThat(result).isNotNull();
        assertThat(result.getAnalysisType()).isEqualTo(AnalysisType.RULE_BASED_FALLBACK);
        assertThat(result.getModelVersion()).isEqualTo("FALLBACK-v1.0");
    }

    // Helper methods
    private List<PaymentRecord> createPaymentHistory() {
        return List.of(
            new PaymentRecord(LocalDateTime.now().minusMonths(1), BigDecimal.valueOf(500), true),
            new PaymentRecord(LocalDateTime.now().minusMonths(2), BigDecimal.valueOf(500), true),
            new PaymentRecord(LocalDateTime.now().minusMonths(3), BigDecimal.valueOf(500), false)
        );
    }

    private Map<String, Object> createApplicationData() {
        return Map.of(
            "income", 65000,
            "employment_length", 24,
            "loan_amount", 40000,
            "home_ownership", "RENT"
        );
    }

    private CustomerProfile createCustomerProfile() {
        return CustomerProfile.builder()
            .customerId("CUST-006")
            .age(35)
            .annualIncome(BigDecimal.valueOf(85000))
            .creditScore(740)
            .employmentType(EmploymentType.FULL_TIME)
            .homeOwnership("OWN")
            .build();
    }

    private LoanAnalysisRequest createLoanAnalysisRequest(String customerId, BigDecimal amount) {
        return LoanAnalysisRequest.builder()
            .customerId(customerId)
            .requestedAmount(amount)
            .loanPurpose(LoanPurpose.PERSONAL)
            .annualIncome(BigDecimal.valueOf(60000))
            .employmentType(EmploymentType.FULL_TIME)
            .creditScore(700)
            .build();
    }
}