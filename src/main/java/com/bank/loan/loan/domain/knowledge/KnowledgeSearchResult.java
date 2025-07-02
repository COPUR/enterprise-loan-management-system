package com.bank.loanmanagement.loan.domain.knowledge;

import java.util.List;
import java.util.Objects;

public class KnowledgeSearchResult {
    
    private final List<BankingKnowledge> results;
    private final int totalResults;
    private final double averageRelevanceScore;
    private final long searchTimeMs;
    private final String queryText;
    
    public KnowledgeSearchResult(List<BankingKnowledge> results, int totalResults, 
                                double averageRelevanceScore, long searchTimeMs, String queryText) {
        this.results = List.copyOf(Objects.requireNonNull(results, "Results cannot be null"));
        this.totalResults = totalResults;
        this.averageRelevanceScore = averageRelevanceScore;
        this.searchTimeMs = searchTimeMs;
        this.queryText = Objects.requireNonNull(queryText, "Query text cannot be null");
    }
    
    public List<BankingKnowledge> getResults() { return results; }
    public int getTotalResults() { return totalResults; }
    public double getAverageRelevanceScore() { return averageRelevanceScore; }
    public long getSearchTimeMs() { return searchTimeMs; }
    public String getQueryText() { return queryText; }
    
    public boolean hasResults() {
        return !results.isEmpty();
    }
    
    public int getResultCount() {
        return results.size();
    }
    
    public boolean isHighQuality() {
        return averageRelevanceScore >= 0.7;
    }
    
    public List<BankingKnowledge> getTopResults(int count) {
        return results.stream()
                .limit(count)
                .toList();
    }
    
    public List<BankingKnowledge> getResultsByCategory(String category) {
        return results.stream()
                .filter(knowledge -> knowledge.isCategory(category))
                .toList();
    }
    
    public List<BankingKnowledge> getResultsByType(KnowledgeType type) {
        return results.stream()
                .filter(knowledge -> knowledge.getType() == type)
                .toList();
    }
    
    @Override
    public String toString() {
        return "KnowledgeSearchResult{" +
                "resultCount=" + results.size() +
                ", totalResults=" + totalResults +
                ", averageRelevanceScore=" + averageRelevanceScore +
                ", searchTimeMs=" + searchTimeMs +
                ", queryText='" + queryText + '\'' +
                '}';
    }
}