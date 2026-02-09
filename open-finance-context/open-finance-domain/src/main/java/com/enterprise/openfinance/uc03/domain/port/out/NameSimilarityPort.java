package com.enterprise.openfinance.uc03.domain.port.out;

public interface NameSimilarityPort {

    int similarityScore(String left, String right);
}
