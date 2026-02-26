package com.enterprise.openfinance.payeeverification.domain.port.out;

public interface NameSimilarityPort {

    int similarityScore(String left, String right);
}
