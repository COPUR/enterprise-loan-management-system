package com.bank.loanmanagement.domain.knowledge;

import java.util.List;

public class BankingKnowledgeService {
    
    public static final double DEFAULT_RELEVANCE_THRESHOLD = 0.5;
    public static final int DEFAULT_MAX_RESULTS = 10;
    
    public KnowledgeSearchResult searchKnowledge(KnowledgeQuery query, 
                                               List<BankingKnowledge> allKnowledge) {
        long startTime = System.currentTimeMillis();
        
        List<BankingKnowledge> filteredResults = filterKnowledge(query, allKnowledge);
        List<BankingKnowledge> rankedResults = rankByRelevance(filteredResults, query);
        List<BankingKnowledge> limitedResults = limitResults(rankedResults, query.getMaxResults());
        
        long searchTime = System.currentTimeMillis() - startTime;
        double averageScore = calculateAverageRelevance(limitedResults);
        
        return new KnowledgeSearchResult(
            limitedResults,
            filteredResults.size(),
            averageScore,
            searchTime,
            query.getQueryText()
        );
    }
    
    public boolean isKnowledgeRelevant(BankingKnowledge knowledge, String query, double threshold) {
        return knowledge.isRelevant(threshold) && 
               containsQueryTerms(knowledge, query);
    }
    
    public List<BankingKnowledge> filterByCategory(List<BankingKnowledge> knowledge, String category) {
        return knowledge.stream()
                .filter(k -> k.isCategory(category))
                .toList();
    }
    
    public List<BankingKnowledge> filterByType(List<BankingKnowledge> knowledge, KnowledgeType type) {
        return knowledge.stream()
                .filter(k -> k.getType() == type)
                .toList();
    }
    
    public List<BankingKnowledge> filterByTags(List<BankingKnowledge> knowledge, List<String> tags) {
        return knowledge.stream()
                .filter(k -> tags.stream().anyMatch(k::hasTag))
                .toList();
    }
    
    private List<BankingKnowledge> filterKnowledge(KnowledgeQuery query, List<BankingKnowledge> knowledge) {
        return knowledge.stream()
                .filter(k -> k.getRelevanceScore() >= query.getMinRelevanceScore())
                .filter(k -> matchesCategories(k, query.getCategories()))
                .filter(k -> matchesTags(k, query.getTags()))
                .filter(k -> matchesTypes(k, query.getTypes()))
                .filter(k -> containsQueryTerms(k, query.getQueryText()))
                .toList();
    }
    
    private List<BankingKnowledge> rankByRelevance(List<BankingKnowledge> knowledge, KnowledgeQuery query) {
        return knowledge.stream()
                .sorted((k1, k2) -> Double.compare(k2.getRelevanceScore(), k1.getRelevanceScore()))
                .toList();
    }
    
    private List<BankingKnowledge> limitResults(List<BankingKnowledge> knowledge, int maxResults) {
        return knowledge.stream()
                .limit(maxResults)
                .toList();
    }
    
    private double calculateAverageRelevance(List<BankingKnowledge> knowledge) {
        return knowledge.stream()
                .mapToDouble(BankingKnowledge::getRelevanceScore)
                .average()
                .orElse(0.0);
    }
    
    private boolean matchesCategories(BankingKnowledge knowledge, List<String> categories) {
        return categories.isEmpty() || 
               categories.stream().anyMatch(knowledge::isCategory);
    }
    
    private boolean matchesTags(BankingKnowledge knowledge, List<String> tags) {
        return tags.isEmpty() || 
               tags.stream().anyMatch(knowledge::hasTag);
    }
    
    private boolean matchesTypes(BankingKnowledge knowledge, List<KnowledgeType> types) {
        return types.isEmpty() || 
               types.contains(knowledge.getType());
    }
    
    private boolean containsQueryTerms(BankingKnowledge knowledge, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        String content = (knowledge.getTitle() + " " + knowledge.getContent()).toLowerCase();
        
        return content.contains(lowerQuery) ||
               knowledge.getTags().stream()
                       .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery));
    }
}