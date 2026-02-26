package com.enterprise.openfinance.payeeverification.infrastructure.matching;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class LevenshteinNameSimilarityAdapterTest {

    private final LevenshteinNameSimilarityAdapter adapter = new LevenshteinNameSimilarityAdapter();

    @Test
    void shouldReturnHundredForExactNormalizedMatch() {
        int score = adapter.similarityScore("Al Tareq Trading LLC", "al  tareq trading llc");

        assertThat(score).isEqualTo(100);
    }

    @Test
    void shouldReturnHighScoreForCloseMatch() {
        int score = adapter.similarityScore("Al Tariq Trading LLC", "Al Tareq Trading LLC");

        assertThat(score).isGreaterThanOrEqualTo(85);
    }

    @Test
    void shouldReturnLowScoreForDifferentNames() {
        int score = adapter.similarityScore("Random Corp", "Al Tareq Trading LLC");

        assertThat(score).isLessThan(70);
    }

    @Test
    void shouldHandleNullInputs() {
        assertThat(adapter.similarityScore(null, null)).isEqualTo(100);
        assertThat(adapter.similarityScore(null, "value")).isEqualTo(0);
    }
}
