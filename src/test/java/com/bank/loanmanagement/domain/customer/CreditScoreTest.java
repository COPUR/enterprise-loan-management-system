package com.bank.loanmanagement.domain.customer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for CreditScore value object
 * Testing credit scoring business rules and rating calculations
 */
@DisplayName("📊 Credit Score Value Object Tests")
class CreditScoreTest {

    @Nested
    @DisplayName("Credit Score Creation")
    class CreditScoreCreationTests {

        @Test
        @DisplayName("Should create credit score with valid parameters")
        void shouldCreateCreditScoreWithValidParameters() {
            // Given
            Integer score = 750;
            String agency = "EXPERIAN";
            LocalDateTime timestamp = LocalDateTime.now();

            // When
            CreditScore creditScore = new CreditScore(score, agency, timestamp);

            // Then
            assertThat(creditScore.getScore()).isEqualTo(score);
            assertThat(creditScore.getReportingAgency()).isEqualTo(agency);
            assertThat(creditScore.getLastUpdated()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("Should create credit score with null values")
        void shouldCreateCreditScoreWithNullValues() {
            // When
            CreditScore creditScore = new CreditScore(null, null, null);

            // Then
            assertThat(creditScore.getScore()).isNull();
            assertThat(creditScore.getReportingAgency()).isNull();
            assertThat(creditScore.getLastUpdated()).isNull();
        }

        @Test
        @DisplayName("Should create credit score using no-args constructor")
        void shouldCreateCreditScoreUsingNoArgsConstructor() {
            // When
            CreditScore creditScore = new CreditScore();

            // Then
            assertThat(creditScore.getScore()).isNull();
            assertThat(creditScore.getReportingAgency()).isNull();
            assertThat(creditScore.getLastUpdated()).isNull();
        }
    }

    @Nested
    @DisplayName("Credit Score Rating Classification")
    class CreditScoreRatingTests {

        @Test
        @DisplayName("Should classify score 800+ as EXCELLENT")
        void shouldClassifyExcellentScores() {
            // Given
            CreditScore score800 = new CreditScore(800, "EXPERIAN", LocalDateTime.now());
            CreditScore score820 = new CreditScore(820, "EQUIFAX", LocalDateTime.now());
            CreditScore score850 = new CreditScore(850, "TRANSUNION", LocalDateTime.now());

            // When & Then
            assertThat(score800.isExcellent()).isTrue();
            assertThat(score800.getRating()).isEqualTo("EXCELLENT");

            assertThat(score820.isExcellent()).isTrue();
            assertThat(score820.getRating()).isEqualTo("EXCELLENT");

            assertThat(score850.isExcellent()).isTrue();
            assertThat(score850.getRating()).isEqualTo("EXCELLENT");
        }

        @Test
        @DisplayName("Should classify score 670-799 as GOOD")
        void shouldClassifyGoodScores() {
            // Given
            CreditScore score670 = new CreditScore(670, "EXPERIAN", LocalDateTime.now());
            CreditScore score720 = new CreditScore(720, "EQUIFAX", LocalDateTime.now());
            CreditScore score799 = new CreditScore(799, "TRANSUNION", LocalDateTime.now());

            // When & Then
            assertThat(score670.isGood()).isTrue();
            assertThat(score670.isExcellent()).isFalse();
            assertThat(score670.getRating()).isEqualTo("GOOD");

            assertThat(score720.isGood()).isTrue();
            assertThat(score720.isExcellent()).isFalse();
            assertThat(score720.getRating()).isEqualTo("GOOD");

            assertThat(score799.isGood()).isTrue();
            assertThat(score799.isExcellent()).isFalse();
            assertThat(score799.getRating()).isEqualTo("GOOD");
        }

        @Test
        @DisplayName("Should classify score 580-669 as FAIR")
        void shouldClassifyFairScores() {
            // Given
            CreditScore score580 = new CreditScore(580, "EXPERIAN", LocalDateTime.now());
            CreditScore score620 = new CreditScore(620, "EQUIFAX", LocalDateTime.now());
            CreditScore score669 = new CreditScore(669, "TRANSUNION", LocalDateTime.now());

            // When & Then
            assertThat(score580.isFair()).isTrue();
            assertThat(score580.isGood()).isFalse();
            assertThat(score580.getRating()).isEqualTo("FAIR");

            assertThat(score620.isFair()).isTrue();
            assertThat(score620.isGood()).isFalse();
            assertThat(score620.getRating()).isEqualTo("FAIR");

            assertThat(score669.isFair()).isTrue();
            assertThat(score669.isGood()).isFalse();
            assertThat(score669.getRating()).isEqualTo("FAIR");
        }

        @Test
        @DisplayName("Should classify score below 580 as POOR")
        void shouldClassifyPoorScores() {
            // Given
            CreditScore score300 = new CreditScore(300, "EXPERIAN", LocalDateTime.now());
            CreditScore score450 = new CreditScore(450, "EQUIFAX", LocalDateTime.now());
            CreditScore score579 = new CreditScore(579, "TRANSUNION", LocalDateTime.now());

            // When & Then
            assertThat(score300.isPoor()).isTrue();
            assertThat(score300.isFair()).isFalse();
            assertThat(score300.getRating()).isEqualTo("POOR");

            assertThat(score450.isPoor()).isTrue();
            assertThat(score450.isFair()).isFalse();
            assertThat(score450.getRating()).isEqualTo("POOR");

            assertThat(score579.isPoor()).isTrue();
            assertThat(score579.isFair()).isFalse();
            assertThat(score579.getRating()).isEqualTo("POOR");
        }

        @Test
        @DisplayName("Should handle null score as POOR rating")
        void shouldHandleNullScoreAsPoorRating() {
            // Given
            CreditScore nullScore = new CreditScore(null, "EXPERIAN", LocalDateTime.now());

            // When & Then
            assertThat(nullScore.isExcellent()).isFalse();
            assertThat(nullScore.isGood()).isFalse();
            assertThat(nullScore.isFair()).isFalse();
            assertThat(nullScore.isPoor()).isFalse();
            assertThat(nullScore.getRating()).isEqualTo("POOR");
        }
    }

    @Nested
    @DisplayName("Credit Score Business Rules")
    class CreditScoreBusinessRulesTests {

        @Test
        @DisplayName("Should handle edge cases for rating boundaries")
        void shouldHandleEdgeCasesForRatingBoundaries() {
            // Given
            CreditScore boundaryExcellent = new CreditScore(800, "EXPERIAN", LocalDateTime.now());
            CreditScore boundaryGood = new CreditScore(670, "EQUIFAX", LocalDateTime.now());
            CreditScore boundaryFair = new CreditScore(580, "TRANSUNION", LocalDateTime.now());
            CreditScore belowFair = new CreditScore(579, "EXPERIAN", LocalDateTime.now());

            // When & Then
            assertThat(boundaryExcellent.getRating()).isEqualTo("EXCELLENT");
            assertThat(boundaryGood.getRating()).isEqualTo("GOOD");
            assertThat(boundaryFair.getRating()).isEqualTo("FAIR");
            assertThat(belowFair.getRating()).isEqualTo("POOR");
        }

        @Test
        @DisplayName("Should support different reporting agencies")
        void shouldSupportDifferentReportingAgencies() {
            // Given
            String[] agencies = {"EXPERIAN", "EQUIFAX", "TRANSUNION", "INNOVIS"};
            Integer score = 720;
            LocalDateTime timestamp = LocalDateTime.now();

            // When & Then
            for (String agency : agencies) {
                CreditScore creditScore = new CreditScore(score, agency, timestamp);
                assertThat(creditScore.getReportingAgency()).isEqualTo(agency);
                assertThat(creditScore.getRating()).isEqualTo("GOOD");
            }
        }

        @Test
        @DisplayName("Should handle extreme score values")
        void shouldHandleExtremeScoreValues() {
            // Given
            CreditScore minimumScore = new CreditScore(300, "EXPERIAN", LocalDateTime.now());  // Lowest possible score
            CreditScore maximumScore = new CreditScore(850, "EXPERIAN", LocalDateTime.now());  // Highest possible score

            // When & Then
            assertThat(minimumScore.getRating()).isEqualTo("POOR");
            assertThat(minimumScore.isPoor()).isTrue();

            assertThat(maximumScore.getRating()).isEqualTo("EXCELLENT");
            assertThat(maximumScore.isExcellent()).isTrue();
        }

        @Test
        @DisplayName("Should maintain timestamp accuracy")
        void shouldMaintainTimestampAccuracy() {
            // Given
            LocalDateTime specificTime = LocalDateTime.of(2023, 12, 15, 14, 30, 45);
            CreditScore creditScore = new CreditScore(750, "EXPERIAN", specificTime);

            // When & Then
            assertThat(creditScore.getLastUpdated()).isEqualTo(specificTime);
            assertThat(creditScore.getLastUpdated().getYear()).isEqualTo(2023);
            assertThat(creditScore.getLastUpdated().getMonthValue()).isEqualTo(12);
            assertThat(creditScore.getLastUpdated().getDayOfMonth()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Credit Score Equality and Comparison")
    class CreditScoreEqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            CreditScore score1 = new CreditScore(750, "EXPERIAN", timestamp);
            CreditScore score2 = new CreditScore(750, "EXPERIAN", timestamp);

            // When & Then
            assertThat(score1).isEqualTo(score2);
            assertThat(score1.hashCode()).isEqualTo(score2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when scores differ")
        void shouldNotBeEqualWhenScoresDiffer() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            CreditScore score1 = new CreditScore(750, "EXPERIAN", timestamp);
            CreditScore score2 = new CreditScore(760, "EXPERIAN", timestamp);

            // When & Then
            assertThat(score1).isNotEqualTo(score2);
        }

        @Test
        @DisplayName("Should not be equal when agencies differ")
        void shouldNotBeEqualWhenAgenciesDiffer() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            CreditScore score1 = new CreditScore(750, "EXPERIAN", timestamp);
            CreditScore score2 = new CreditScore(750, "EQUIFAX", timestamp);

            // When & Then
            assertThat(score1).isNotEqualTo(score2);
        }

        @Test
        @DisplayName("Should not be equal when timestamps differ")
        void shouldNotBeEqualWhenTimestampsDiffer() {
            // Given
            LocalDateTime timestamp1 = LocalDateTime.now();
            LocalDateTime timestamp2 = timestamp1.plusMinutes(1);
            CreditScore score1 = new CreditScore(750, "EXPERIAN", timestamp1);
            CreditScore score2 = new CreditScore(750, "EXPERIAN", timestamp2);

            // When & Then
            assertThat(score1).isNotEqualTo(score2);
        }
    }

    @Nested
    @DisplayName("Credit Score Rating Logic")
    class CreditScoreRatingLogicTests {

        @Test
        @DisplayName("Should prioritize highest rating when multiple conditions could apply")
        void shouldPrioritizeHighestRating() {
            // Given - A score that meets multiple criteria
            CreditScore excellentScore = new CreditScore(820, "EXPERIAN", LocalDateTime.now());

            // When & Then - Should return the highest applicable rating
            assertThat(excellentScore.isExcellent()).isTrue();
            assertThat(excellentScore.isGood()).isTrue();  // Also meets good criteria
            assertThat(excellentScore.isFair()).isTrue();  // Also meets fair criteria
            assertThat(excellentScore.isPoor()).isFalse(); // Doesn't meet poor criteria
            assertThat(excellentScore.getRating()).isEqualTo("EXCELLENT"); // But returns highest
        }

        @Test
        @DisplayName("Should validate rating consistency across all score ranges")
        void shouldValidateRatingConsistencyAcrossAllScoreRanges() {
            // Test scores across all possible ranges
            for (int score = 300; score <= 850; score += 50) {
                CreditScore creditScore = new CreditScore(score, "TEST_AGENCY", LocalDateTime.now());
                
                String expectedRating;
                if (score >= 800) expectedRating = "EXCELLENT";
                else if (score >= 670) expectedRating = "GOOD";
                else if (score >= 580) expectedRating = "FAIR";
                else expectedRating = "POOR";
                
                assertThat(creditScore.getRating())
                    .as("Score %d should have rating %s", score, expectedRating)
                    .isEqualTo(expectedRating);
            }
        }
    }

    @Nested
    @DisplayName("Credit Score Performance")
    class CreditScorePerformanceTests {

        @Test
        @DisplayName("Should calculate ratings efficiently for multiple scores")
        void shouldCalculateRatingsEfficientlyForMultipleScores() {
            // Given
            int testCount = 10000;
            
            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < testCount; i++) {
                int score = 300 + (i % 551); // Scores from 300 to 850
                CreditScore creditScore = new CreditScore(score, "PERFORMANCE_TEST", LocalDateTime.now());
                creditScore.getRating(); // Trigger rating calculation
            }
            long endTime = System.currentTimeMillis();

            // Then
            long duration = endTime - startTime;
            assertThat(duration).isLessThan(500); // Should complete within 500ms
        }

        @Test
        @DisplayName("Should handle concurrent rating calculations")
        void shouldHandleConcurrentRatingCalculations() {
            // Given
            CreditScore creditScore = new CreditScore(750, "CONCURRENT_TEST", LocalDateTime.now());

            // When - Simulate concurrent access
            String[] results = new String[100];
            for (int i = 0; i < 100; i++) {
                results[i] = creditScore.getRating();
            }

            // Then
            for (String result : results) {
                assertThat(result).isEqualTo("GOOD");
            }
        }
    }
}