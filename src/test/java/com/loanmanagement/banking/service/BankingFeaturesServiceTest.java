package com.loanmanagement.banking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Java 21 Banking Features Service
 * 
 * Comprehensive testing of Java 21 features in banking context:
 * - Virtual Threads for high-frequency trading
 * - Pattern Matching for customer risk assessment
 * - Sequenced Collections for transaction ordering
 * - Record Patterns for payment validation
 * - Enhanced switch expressions for fraud detection
 * 
 * Banking scenarios covered:
 * - High-frequency trading operations
 * - Customer risk profiling
 * - Transaction analysis
 * - Payment validation
 * - Regulatory compliance
 * - Fraud detection
 */
@DisplayName("Java 21 Banking Features Service")
class BankingFeaturesServiceTest {
    
    private BankingFeaturesService bankingFeaturesService;
    
    @BeforeEach
    void setUp() {
        bankingFeaturesService = new BankingFeaturesService();
    }
    
    @Nested
    @DisplayName("Virtual Threads High-Frequency Trading")
    class VirtualThreadsHighFrequencyTrading {
        
        @Test
        @DisplayName("Should process market data updates with Virtual Threads efficiently")
        void shouldProcessMarketDataUpdatesWithVirtualThreadsEfficiently() {
            // Given
            var marketUpdates = generateMarketDataUpdates(1000);
            
            // When
            var startTime = System.currentTimeMillis();
            var result = bankingFeaturesService.processHighFrequencyTrading(marketUpdates);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(result.totalUpdates()).isEqualTo(1000);
            assertThat(result.successfulUpdates()).isGreaterThan(900); // Expect > 90% success rate
            assertThat(result.processingTimeMs()).isLessThan(2000); // Should process 1000 updates within 2 seconds
            assertThat(processingTime).isLessThan(3000); // Total time including setup
            
            // Verify results structure
            assertThat(result.results()).hasSize(1000);
            assertThat(result.results()).allMatch(updateResult -> 
                updateResult.status() != null && updateResult.processedAt() != null
            );
        }
        
        @Test
        @DisplayName("Should demonstrate scalability with large market data volumes")
        void shouldDemonstrateScalabilityWithLargeMarketDataVolumes() {
            // Given
            var largeMarketBatch = generateMarketDataUpdates(5000);
            
            // When
            var result = bankingFeaturesService.processHighFrequencyTrading(largeMarketBatch);
            
            // Then
            assertThat(result.totalUpdates()).isEqualTo(5000);
            assertThat(result.processingTimeMs()).isLessThan(10000); // Virtual Threads should handle 5000 updates efficiently
            
            // High success rate even under load
            var successRate = (double) result.successfulUpdates() / result.totalUpdates();
            assertThat(successRate).isGreaterThan(0.85); // > 85% success rate
        }
        
        @Test
        @DisplayName("Should handle concurrent market data processing")
        void shouldHandleConcurrentMarketDataProcessing() {
            // Given
            var batch1 = generateMarketDataUpdates(100);
            var batch2 = generateMarketDataUpdates(100);
            var batch3 = generateMarketDataUpdates(100);
            
            // When - Process batches concurrently
            var future1 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                bankingFeaturesService.processHighFrequencyTrading(batch1)
            );
            var future2 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                bankingFeaturesService.processHighFrequencyTrading(batch2)
            );
            var future3 = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                bankingFeaturesService.processHighFrequencyTrading(batch3)
            );
            
            // Then
            var results = List.of(
                future1.join(),
                future2.join(),
                future3.join()
            );
            
            assertThat(results).hasSize(3);
            assertThat(results).allMatch(result -> 
                result.totalUpdates() == 100 && result.successfulUpdates() > 80
            );
        }
    }
    
    @Nested
    @DisplayName("Pattern Matching Customer Risk Assessment")
    class PatternMatchingCustomerRiskAssessment {
        
        @Test
        @DisplayName("Should assess premium customer risk with pattern matching")
        void shouldAssessPremiumCustomerRiskWithPatternMatching() {
            // Given
            var premiumCustomer = new BankingFeaturesService.PremiumCustomer(
                "PREM001",
                BigDecimal.valueOf(250000), // High income
                780, // Excellent credit
                5 // 5 years relationship
            );
            
            // When
            var riskAssessment = bankingFeaturesService.assessCustomerRisk(premiumCustomer);
            
            // Then
            assertThat(riskAssessment.riskLevel()).isEqualTo(BankingFeaturesService.RiskLevel.LOW);
            assertThat(riskAssessment.confidence()).isGreaterThan(0.9);
            assertThat(riskAssessment.reasoning()).contains("Premium customer");
            assertThat(riskAssessment.recommendedInterestRate()).isLessThan(BigDecimal.valueOf(4.0));
        }
        
        @Test
        @DisplayName("Should assess standard customer risk appropriately")
        void shouldAssessStandardCustomerRiskAppropriately() {
            // Given
            var standardCustomer = new BankingFeaturesService.StandardCustomer(
                "STD001",
                BigDecimal.valueOf(80000), // Moderate income
                720, // Good credit
                BankingFeaturesService.EmploymentType.FULL_TIME
            );
            
            // When
            var riskAssessment = bankingFeaturesService.assessCustomerRisk(standardCustomer);
            
            // Then
            assertThat(riskAssessment.riskLevel()).isEqualTo(BankingFeaturesService.RiskLevel.MEDIUM);
            assertThat(riskAssessment.confidence()).isGreaterThan(0.7);
            assertThat(riskAssessment.reasoning()).contains("Standard customer");
            assertThat(riskAssessment.recommendedInterestRate()).isBetween(BigDecimal.valueOf(3.0), BigDecimal.valueOf(6.0));
        }
        
        @Test
        @DisplayName("Should assess young customer with growth potential")
        void shouldAssessYoungCustomerWithGrowthPotential() {
            // Given
            var youngCustomer = new BankingFeaturesService.YoungCustomer(
                "YOUNG001",
                28, // Young age
                BigDecimal.valueOf(65000),
                BankingFeaturesService.EducationLevel.GRADUATE
            );
            
            // When
            var riskAssessment = bankingFeaturesService.assessCustomerRisk(youngCustomer);
            
            // Then
            assertThat(riskAssessment.riskLevel()).isEqualTo(BankingFeaturesService.RiskLevel.MEDIUM);
            assertThat(riskAssessment.reasoning()).contains("growth potential");
        }
        
        @Test
        @DisplayName("Should assess business customer based on industry risk")
        void shouldAssessBusinessCustomerBasedOnIndustryRisk() {
            // Given
            var techBusinessCustomer = new BankingFeaturesService.BusinessCustomer(
                "BIZ001",
                BigDecimal.valueOf(2000000), // High revenue
                5, // Established business
                BankingFeaturesService.Industry.TECHNOLOGY
            );
            
            // When
            var riskAssessment = bankingFeaturesService.assessCustomerRisk(techBusinessCustomer);
            
            // Then
            assertThat(riskAssessment.riskLevel()).isEqualTo(BankingFeaturesService.RiskLevel.LOW);
            assertThat(riskAssessment.reasoning()).contains("Established business");
            
            // Tech industry should get better rates
            assertThat(riskAssessment.recommendedInterestRate()).isLessThan(BigDecimal.valueOf(5.0));
        }
        
        @Test
        @DisplayName("Should handle edge cases in risk assessment pattern matching")
        void shouldHandleEdgeCasesInRiskAssessmentPatternMatching() {
            // Given - Premium customer with poor credit (edge case)
            var problematicPremiumCustomer = new BankingFeaturesService.PremiumCustomer(
                "PREM_POOR",
                BigDecimal.valueOf(300000), // High income
                580, // Poor credit score
                10 // Long relationship
            );
            
            // When
            var riskAssessment = bankingFeaturesService.assessCustomerRisk(problematicPremiumCustomer);
            
            // Then - Should catch this edge case with pattern matching
            assertThat(riskAssessment.riskLevel()).isEqualTo(BankingFeaturesService.RiskLevel.HIGH);
            assertThat(riskAssessment.reasoning()).contains("credit concerns");
            assertThat(riskAssessment.recommendedInterestRate()).isGreaterThan(BigDecimal.valueOf(8.0));
        }
    }
    
    @Nested
    @DisplayName("Sequenced Collections Transaction Analysis")
    class SequencedCollectionsTransactionAnalysis {
        
        @Test
        @DisplayName("Should maintain transaction order with Sequenced Collections")
        void shouldMaintainTransactionOrderWithSequencedCollections() {
            // Given - Transactions in specific chronological order
            var transactions = List.of(
                createTransaction("TXN001", BigDecimal.valueOf(100), LocalDateTime.now().minusHours(5)),
                createTransaction("TXN002", BigDecimal.valueOf(200), LocalDateTime.now().minusHours(4)),
                createTransaction("TXN003", BigDecimal.valueOf(300), LocalDateTime.now().minusHours(3)),
                createTransaction("TXN004", BigDecimal.valueOf(400), LocalDateTime.now().minusHours(2)),
                createTransaction("TXN005", BigDecimal.valueOf(500), LocalDateTime.now().minusHours(1))
            );
            
            // When
            var analysis = bankingFeaturesService.analyzeTransactionSequence(transactions);
            
            // Then - Should preserve chronological order
            assertThat(analysis.transactionCount()).isEqualTo(5);
            assertThat(analysis.totalAmount()).isEqualTo(BigDecimal.valueOf(1500));
            assertThat(analysis.averageAmount()).isEqualTo(BigDecimal.valueOf(300.00));
            
            // First and last transactions should be correctly identified
            assertThat(analysis.firstTransaction()).isBefore(analysis.lastTransaction());
        }
        
        @Test
        @DisplayName("Should detect unusual transaction patterns")
        void shouldDetectUnusualTransactionPatterns() {
            // Given - Mix of normal and unusual transactions
            var transactions = List.of(
                createTransaction("TXN001", BigDecimal.valueOf(100), LocalDateTime.now().minusHours(6)),
                createTransaction("TXN002", BigDecimal.valueOf(15000), LocalDateTime.now().minusHours(5)), // Unusual - large
                createTransaction("TXN003", BigDecimal.valueOf(200), LocalDateTime.now().minusHours(4)),
                createTransaction("TXN004", BigDecimal.valueOf(25000), LocalDateTime.now().minusHours(3)), // Unusual - large
                createTransaction("TXN005", BigDecimal.valueOf(150), LocalDateTime.now().minusHours(2))
            );
            
            // When
            var analysis = bankingFeaturesService.analyzeTransactionSequence(transactions);
            
            // Then
            assertThat(analysis.unusualTransactions()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(analysis.unusualTransactions()).allMatch(txn -> 
                txn.amount().compareTo(BigDecimal.valueOf(10000)) > 0
            );
        }
        
        @Test
        @DisplayName("Should handle large transaction volumes efficiently")
        void shouldHandleLargeTransactionVolumesEfficiently() {
            // Given
            var largeTransactionSet = generateTransactions(1000);
            
            // When
            var startTime = System.currentTimeMillis();
            var analysis = bankingFeaturesService.analyzeTransactionSequence(largeTransactionSet);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(analysis.transactionCount()).isEqualTo(1000);
            assertThat(processingTime).isLessThan(1000); // Should analyze 1000 transactions within 1 second
            assertThat(analysis.totalAmount()).isGreaterThan(BigDecimal.ZERO);
            assertThat(analysis.averageAmount()).isGreaterThan(BigDecimal.ZERO);
        }
    }
    
    @Nested
    @DisplayName("Record Patterns Payment Validation")
    class RecordPatternsPaymentValidation {
        
        @Test
        @DisplayName("Should validate payment requests with record patterns")
        void shouldValidatePaymentRequestsWithRecordPatterns() {
            // Given - Valid payment request
            var validPayment = new BankingFeaturesService.PaymentRequest(
                "PAY001",
                BigDecimal.valueOf(5000),
                "1234567890",
                "0987654321",
                BankingFeaturesService.PaymentType.DOMESTIC
            );
            
            // When
            var validationResult = bankingFeaturesService.validatePaymentRequest(validPayment);
            
            // Then
            assertThat(validationResult.isValid()).isTrue();
            assertThat(validationResult.status()).isEqualTo(BankingFeaturesService.ValidationStatus.VALID);
            assertThat(validationResult.message()).contains("validation passed");
        }
        
        @Test
        @DisplayName("Should reject payment with negative amount using record patterns")
        void shouldRejectPaymentWithNegativeAmountUsingRecordPatterns() {
            // Given - Payment with negative amount
            var invalidPayment = new BankingFeaturesService.PaymentRequest(
                "PAY002",
                BigDecimal.valueOf(-1000), // Negative amount
                "1234567890",
                "0987654321",
                BankingFeaturesService.PaymentType.DOMESTIC
            );
            
            // When
            var validationResult = bankingFeaturesService.validatePaymentRequest(invalidPayment);
            
            // Then
            assertThat(validationResult.isValid()).isFalse();
            assertThat(validationResult.status()).isEqualTo(BankingFeaturesService.ValidationStatus.INVALID);
            assertThat(validationResult.message()).contains("positive");
        }
        
        @Test
        @DisplayName("Should flag large international transfers for approval")
        void shouldFlagLargeInternationalTransfersForApproval() {
            // Given - Large international transfer
            var largeInternationalPayment = new BankingFeaturesService.PaymentRequest(
                "PAY003",
                BigDecimal.valueOf(75000), // Large amount
                "1234567890",
                "0987654321",
                BankingFeaturesService.PaymentType.INTERNATIONAL
            );
            
            // When
            var validationResult = bankingFeaturesService.validatePaymentRequest(largeInternationalPayment);
            
            // Then
            assertThat(validationResult.isValid()).isFalse();
            assertThat(validationResult.status()).isEqualTo(BankingFeaturesService.ValidationStatus.REQUIRES_APPROVAL);
            assertThat(validationResult.message()).contains("manual approval");
        }
        
        @Test
        @DisplayName("Should reject domestic payment with same source and destination")
        void shouldRejectDomesticPaymentWithSameSourceAndDestination() {
            // Given - Payment with same from/to account
            var sameAccountPayment = new BankingFeaturesService.PaymentRequest(
                "PAY004",
                BigDecimal.valueOf(1000),
                "1234567890",
                "1234567890", // Same account
                BankingFeaturesService.PaymentType.DOMESTIC
            );
            
            // When
            var validationResult = bankingFeaturesService.validatePaymentRequest(sameAccountPayment);
            
            // Then
            assertThat(validationResult.isValid()).isFalse();
            assertThat(validationResult.status()).isEqualTo(BankingFeaturesService.ValidationStatus.INVALID);
            assertThat(validationResult.message()).contains("cannot be the same");
        }
    }
    
    @Nested
    @DisplayName("Concurrent Regulatory Compliance")
    class ConcurrentRegulatoryCompliance {
        
        @Test
        @DisplayName("Should check multiple compliance frameworks concurrently")
        void shouldCheckMultipleComplianceFrameworksConcurrently() {
            // Given
            var transactionData = createCompliantTransactionData();
            
            // When
            var startTime = System.currentTimeMillis();
            var complianceResult = bankingFeaturesService.checkRegulatoryCompliance(transactionData);
            var processingTime = System.currentTimeMillis() - startTime;
            
            // Then
            assertThat(complianceResult.overallCompliant()).isTrue();
            assertThat(complianceResult.violations()).isEmpty();
            assertThat(complianceResult.detailedResults()).hasSize(4); // PCI DSS, GDPR, SOX, FAPI
            assertThat(processingTime).isLessThan(5000); // Should complete within 5 seconds
            
            // Verify all frameworks were checked
            assertThat(complianceResult.detailedResults()).containsKeys("PCI_DSS", "GDPR", "SOX", "FAPI");
        }
        
        @Test
        @DisplayName("Should detect compliance violations")
        void shouldDetectComplianceViolations() {
            // Given - Transaction data with compliance issues
            var nonCompliantTransactionData = new BankingFeaturesService.TransactionData(
                "TXN_VIOLATION",
                true,  // Contains card data
                false, // Not encrypted - PCI DSS violation
                true,  // Contains personal data
                false, // No consent - GDPR violation
                true,  // Financial transaction
                false, // No audit trail - SOX violation
                true,  // Open banking transaction
                false  // No FAPI headers - FAPI violation
            );
            
            // When
            var complianceResult = bankingFeaturesService.checkRegulatoryCompliance(nonCompliantTransactionData);
            
            // Then
            assertThat(complianceResult.overallCompliant()).isFalse();
            assertThat(complianceResult.violations()).hasSizeGreaterThan(0);
            
            // Should detect multiple violations
            assertThat(complianceResult.violations()).anyMatch(violation -> violation.contains("PCI DSS"));
            assertThat(complianceResult.violations()).anyMatch(violation -> violation.contains("GDPR"));
            assertThat(complianceResult.violations()).anyMatch(violation -> violation.contains("SOX"));
            assertThat(complianceResult.violations()).anyMatch(violation -> violation.contains("FAPI"));
        }
    }
    
    @Nested
    @DisplayName("Advanced Pattern Matching Fraud Detection")
    class AdvancedPatternMatchingFraudDetection {
        
        @Test
        @DisplayName("Should detect high-risk fraud patterns")
        void shouldDetectHighRiskFraudPatterns() {
            // Given - Suspicious transaction pattern
            var suspiciousTransaction = new BankingFeaturesService.Transaction(
                "TXN_SUSPICIOUS",
                BigDecimal.valueOf(75000), // Large amount
                BankingFeaturesService.TransactionType.CASH_WITHDRAWAL,
                "UNKNOWN_LOCATION", // Unusual location
                LocalDateTime.now().withHour(2) // Night time
            );
            
            var customerBehavior = new BankingFeaturesService.CustomerBehavior(
                List.of("HOME", "OFFICE"), // Usual locations
                BigDecimal.valueOf(1000), // Normal transaction amount
                2 // Normal transaction frequency
            );
            
            // When
            var fraudResult = bankingFeaturesService.detectFraud(suspiciousTransaction, customerBehavior);
            
            // Then
            assertThat(fraudResult.fraudScore()).isGreaterThan(0.7);
            assertThat(fraudResult.riskLevel()).isIn(
                BankingFeaturesService.FraudRiskLevel.HIGH,
                BankingFeaturesService.FraudRiskLevel.MEDIUM
            );
            assertThat(fraudResult.actionsRequired()).isNotEmpty();
        }
        
        @Test
        @DisplayName("Should identify normal transaction patterns")
        void shouldIdentifyNormalTransactionPatterns() {
            // Given - Normal transaction
            var normalTransaction = new BankingFeaturesService.Transaction(
                "TXN_NORMAL",
                BigDecimal.valueOf(500), // Normal amount
                BankingFeaturesService.TransactionType.PURCHASE,
                "HOME", // Usual location
                LocalDateTime.now().withHour(14) // Afternoon
            );
            
            var customerBehavior = new BankingFeaturesService.CustomerBehavior(
                List.of("HOME", "OFFICE"),
                BigDecimal.valueOf(600), // Similar to transaction amount
                3 // Normal frequency
            );
            
            // When
            var fraudResult = bankingFeaturesService.detectFraud(normalTransaction, customerBehavior);
            
            // Then
            assertThat(fraudResult.fraudScore()).isLessThan(0.3);
            assertThat(fraudResult.riskLevel()).isEqualTo(BankingFeaturesService.FraudRiskLevel.MINIMAL);
            assertThat(fraudResult.actionsRequired()).isEmpty();
        }
        
        @Test
        @DisplayName("Should detect rapid transaction patterns")
        void shouldDetectRapidTransactionPatterns() {
            // Given - Rapid transaction scenario
            var rapidTransaction = new BankingFeaturesService.Transaction(
                "TXN_RAPID",
                BigDecimal.valueOf(2000),
                BankingFeaturesService.TransactionType.ONLINE_PURCHASE,
                "HOME",
                LocalDateTime.now()
            );
            
            var rapidBehavior = new BankingFeaturesService.CustomerBehavior(
                List.of("HOME", "OFFICE"),
                BigDecimal.valueOf(1000),
                15 // High transaction frequency
            );
            
            // When
            var fraudResult = bankingFeaturesService.detectFraud(rapidTransaction, rapidBehavior);
            
            // Then
            assertThat(fraudResult.fraudScore()).isGreaterThan(0.6);
            assertThat(fraudResult.riskLevel()).isIn(
                BankingFeaturesService.FraudRiskLevel.MEDIUM,
                BankingFeaturesService.FraudRiskLevel.HIGH
            );
            assertThat(fraudResult.reasoning()).contains("Rapid transaction");
        }
    }
    
    // Helper methods
    
    private List<BankingFeaturesService.MarketDataUpdate> generateMarketDataUpdates(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new BankingFeaturesService.MarketDataUpdate(
                "SYMBOL" + (i % 100), // Cycle through 100 symbols
                BigDecimal.valueOf(100 + (i * 0.1)), // Varying prices
                1000L + (i * 10), // Varying volumes
                LocalDateTime.now().minusSeconds(count - i) // Chronological timestamps
            ))
            .toList();
    }
    
    private BankingFeaturesService.Transaction createTransaction(String id, BigDecimal amount, LocalDateTime timestamp) {
        return new BankingFeaturesService.Transaction(
            id,
            amount,
            BankingFeaturesService.TransactionType.PURCHASE,
            "HOME",
            timestamp
        );
    }
    
    private List<BankingFeaturesService.Transaction> generateTransactions(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> new BankingFeaturesService.Transaction(
                "TXN" + String.format("%06d", i),
                BigDecimal.valueOf(100 + (i * 10)),
                BankingFeaturesService.TransactionType.values()[i % BankingFeaturesService.TransactionType.values().length],
                "LOCATION" + (i % 10),
                LocalDateTime.now().minusMinutes(count - i)
            ))
            .toList();
    }
    
    private BankingFeaturesService.TransactionData createCompliantTransactionData() {
        return new BankingFeaturesService.TransactionData(
            "TXN_COMPLIANT",
            true,  // Contains card data
            true,  // Properly encrypted
            true,  // Contains personal data
            true,  // Has proper consent
            true,  // Financial transaction
            true,  // Has audit trail
            true,  // Open banking transaction
            true   // Has FAPI headers
        );
    }
}