package com.enterprise.openfinance.uc03.infrastructure.matching;

import com.enterprise.openfinance.uc03.domain.port.out.NameSimilarityPort;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LevenshteinNameSimilarityAdapter implements NameSimilarityPort {

    @Override
    public int similarityScore(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);

        if (normalizedLeft.isEmpty() && normalizedRight.isEmpty()) {
            return 100;
        }

        int distance = levenshteinDistance(normalizedLeft, normalizedRight);
        int maxLength = Math.max(normalizedLeft.length(), normalizedRight.length());
        if (maxLength == 0) {
            return 100;
        }
        double similarity = 1.0 - ((double) distance / maxLength);
        int score = (int) Math.round(similarity * 100);
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static int levenshteinDistance(String left, String right) {
        int[][] matrix = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            matrix[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            matrix[0][j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                matrix[i][j] = Math.min(
                        Math.min(
                                matrix[i - 1][j] + 1,
                                matrix[i][j - 1] + 1
                        ),
                        matrix[i - 1][j - 1] + cost
                );
            }
        }
        return matrix[left.length()][right.length()];
    }
}
