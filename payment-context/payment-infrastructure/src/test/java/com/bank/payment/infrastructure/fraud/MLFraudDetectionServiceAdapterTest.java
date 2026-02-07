package com.bank.payment.infrastructure.fraud;

import com.bank.payment.application.FraudDetectedException;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.AccountId;
import com.bank.payment.domain.PaymentId;
import com.bank.payment.domain.PaymentType;
import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for ML-based Fraud Detection Service
 * 
 * Tests comprehensive fraud detection scenarios:
 * - ML model integration and ensemble predictions
 * - Velocity and behavioral pattern analysis
 * - Geospatial and network risk assessment
 * - Anomaly detection and risk scoring
 * - Edge cases and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ML Fraud Detection Service Tests")
class MLFraudDetectionServiceAdapterTest {
    
    @Mock
    private MLModelService mlModelService;
    
    @Mock
    private TransactionHistoryService transactionHistoryService;
    
    @Mock
    private BehavioralAnalysisService behavioralAnalysisService;
    
    @Mock
    private AnomalyDetectionService anomalyDetectionService;
    
    @Mock
    private GeospatialAnalysisService geospatialAnalysisService;
    
    @Mock
    private NetworkAnalysisService networkAnalysisService;
    
    private MLFraudDetectionServiceAdapter fraudDetectionService;
    
    @BeforeEach
    void setUp() {
        fraudDetectionService = new MLFraudDetectionServiceAdapter(
            mlModelService,
            transactionHistoryService,
            behavioralAnalysisService,
            anomalyDetectionService,
            geospatialAnalysisService,
            networkAnalysisService
        );
    }
    
    @Test
    @DisplayName("Should allow legitimate low-risk payment")
    void shouldAllowLegitimateLowRiskPayment() {
        // Given
        Payment payment = createTestPayment("CUST-001", BigDecimal.valueOf(100), PaymentType.TRANSFER);
        
        // Mock low risk scores from all services
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(15);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(5);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.1);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(8);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(3);
        
        // When & Then
        assertThatCode(() -> fraudDetectionService.isValidPayment(payment))
            .doesNotThrowAnyException();
        
        assertThat(fraudDetectionService.calculateRiskScore(payment)).isLessThan(75);
    }
    
    @Test
    @DisplayName("Should block high-risk fraudulent payment")
    void shouldBlockHighRiskFraudulentPayment() {
        // Given
        Payment payment = createTestPayment("CUST-002", BigDecimal.valueOf(50000), PaymentType.WIRE_TRANSFER);
        
        // Mock high risk scores from all services
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(85);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(25);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.98);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(40);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(30);
        
        // When & Then
        assertThatThrownBy(() -> fraudDetectionService.isValidPayment(payment))
            .isInstanceOf(FraudDetectedException.class)
            .hasMessageContaining("Payment blocked due to fraud detection");
        
        assertThat(fraudDetectionService.calculateRiskScore(payment)).isGreaterThanOrEqualTo(75);
    }
    
    @Test
    @DisplayName("Should detect velocity limit violations")
    void shouldDetectVelocityLimitViolations() {
        // Given
        Payment payment = createTestPayment("CUST-003", BigDecimal.valueOf(60000), PaymentType.TRANSFER);
        
        // When
        boolean exceedsVelocity = fraudDetectionService.exceedsVelocityLimits(payment);
        
        // Then
        assertThat(exceedsVelocity).isTrue();
    }
    
    @Test
    @DisplayName("Should detect suspicious behavioral patterns")
    void shouldDetectSuspiciousBehavioralPatterns() {
        // Given
        Payment payment = createTestPayment("CUST-004", BigDecimal.valueOf(1000), PaymentType.ACH);
        
        // Mock high behavioral risk
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(45);
        
        // When
        boolean suspiciousPattern = fraudDetectionService.isSuspiciousPattern(payment);
        
        // Then
        assertThat(suspiciousPattern).isTrue();
    }
    
    @Test
    @DisplayName("Should handle ML model service errors gracefully")
    void shouldHandleMLModelServiceErrorsGracefully() {
        // Given
        Payment payment = createTestPayment("CUST-005", BigDecimal.valueOf(1000), PaymentType.TRANSFER);
        
        // Mock ML service throwing exception
        when(mlModelService.predictRiskScore(any(MLFeatures.class)))
            .thenThrow(new RuntimeException("ML service unavailable"));
        
        // When & Then
        assertThatThrownBy(() -> fraudDetectionService.isValidPayment(payment))
            .isInstanceOf(FraudDetectedException.class)
            .hasMessageContaining("Payment blocked due to fraud detection service error");
    }
    
    @Test
    @DisplayName("Should calculate accurate risk scores for different scenarios")
    void shouldCalculateAccurateRiskScoresForDifferentScenarios() {
        // Scenario 1: Low risk
        Payment lowRiskPayment = createTestPayment("CUST-LOW", BigDecimal.valueOf(50), PaymentType.TRANSFER);
        mockLowRiskScenario();
        int lowRiskScore = fraudDetectionService.calculateRiskScore(lowRiskPayment);
        
        // Scenario 2: Medium risk
        Payment mediumRiskPayment = createTestPayment("CUST-MED", BigDecimal.valueOf(5000), PaymentType.ACH);
        mockMediumRiskScenario();
        int mediumRiskScore = fraudDetectionService.calculateRiskScore(mediumRiskPayment);
        
        // Scenario 3: High risk
        Payment highRiskPayment = createTestPayment("CUST-HIGH", BigDecimal.valueOf(100000), PaymentType.WIRE_TRANSFER);
        mockHighRiskScenario();
        int highRiskScore = fraudDetectionService.calculateRiskScore(highRiskPayment);
        
        // Assertions
        assertThat(lowRiskScore).isLessThan(50);
        assertThat(mediumRiskScore).isBetween(50, 95);
        assertThat(highRiskScore).isBetween(95, 100);
        
        // Risk scores should be ordered
        assertThat(lowRiskScore).isLessThan(mediumRiskScore);
        assertThat(mediumRiskScore).isLessThan(highRiskScore);
    }
    
    @Test
    @DisplayName("Should analyze geospatial risks correctly")
    void shouldAnalyzeGeospatialRisksCorrectly() {
        // Given
        Payment payment = createTestPayment("CUST-GEO", BigDecimal.valueOf(1000), PaymentType.TRANSFER);
        
        // Mock high geospatial risk (unusual location)
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(20);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(10);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.2);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(50); // High geo risk
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(5);
        
        // When
        int riskScore = fraudDetectionService.calculateRiskScore(payment);
        
        // Then
        assertThat(riskScore).isGreaterThan(75); // Should be high risk due to geospatial factors
        verify(geospatialAnalysisService).analyzeGeospatialRisk(eq("CUST-GEO"), any());
    }
    
    @Test
    @DisplayName("Should integrate all fraud detection components")
    void shouldIntegrateAllFraudDetectionComponents() {
        // Given
        Payment payment = createTestPayment("CUST-INTEGRATED", BigDecimal.valueOf(10000), PaymentType.WIRE_TRANSFER);
        
        // Mock all services
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(60);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(20);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.7);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(15);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(10);
        
        // When
        fraudDetectionService.calculateRiskScore(payment);
        
        // Then - verify all services were called
        verify(mlModelService).predictRiskScore(any(MLFeatures.class));
        verify(behavioralAnalysisService).analyzeBehavioralPatterns(eq("CUST-INTEGRATED"), any());
        verify(anomalyDetectionService).calculateAnomalyScore(any(MLFeatures.class));
        verify(geospatialAnalysisService).analyzeGeospatialRisk(eq("CUST-INTEGRATED"), any());
        verify(networkAnalysisService).analyzeNetworkRisk(any());
    }
    
    @Test
    @DisplayName("Should handle edge cases for risk calculation")
    void shouldHandleEdgeCasesForRiskCalculation() {
        // Given - Payment with minimal amount
        Payment zeroAmountPayment = createTestPayment("CUST-ZERO", BigDecimal.valueOf(0.01), PaymentType.TRANSFER);
        
        // Mock services
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(0);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(0);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.0);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(0);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(0);
        
        // When
        int riskScore = fraudDetectionService.calculateRiskScore(zeroAmountPayment);
        
        // Then
        assertThat(riskScore).isBetween(0, 100); // Should be within valid range
    }
    
    // Helper methods
    private Payment createTestPayment(String customerId, BigDecimal amount, PaymentType type) {
        return Payment.create(
            PaymentId.of("PAY-" + System.currentTimeMillis()),
            CustomerId.of(customerId),
            AccountId.of("ACC-FROM-001"),
            AccountId.of("ACC-TO-002"),
            Money.usd(amount),
            type,
            "Test payment"
        );
    }
    
    private void mockLowRiskScenario() {
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(10);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(5);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.1);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(3);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(2);
    }
    
    private void mockMediumRiskScenario() {
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(40);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(15);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.5);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(12);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(8);
    }
    
    private void mockHighRiskScenario() {
        when(mlModelService.predictRiskScore(any(MLFeatures.class))).thenReturn(80);
        when(behavioralAnalysisService.analyzeBehavioralPatterns(any(), any())).thenReturn(30);
        when(anomalyDetectionService.calculateAnomalyScore(any())).thenReturn(0.95);
        when(geospatialAnalysisService.analyzeGeospatialRisk(any(), any())).thenReturn(45);
        when(networkAnalysisService.analyzeNetworkRisk(any())).thenReturn(25);
    }
}
