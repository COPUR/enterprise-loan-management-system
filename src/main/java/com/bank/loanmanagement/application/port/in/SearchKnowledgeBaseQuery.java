package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.domain.knowledge.KnowledgeType;

import java.util.List;
import java.util.Objects;

public class SearchKnowledgeBaseQuery {
    
    private final String query;
    private final List<String> categories;
    private final List<String> tags;
    private final List<KnowledgeType> types;
    private final int maxResults;
    private final double minRelevanceScore;
    private final boolean includeContent;
    private final String userId;
    
    public SearchKnowledgeBaseQuery(String query, List<String> categories, List<String> tags,
                                   List<KnowledgeType> types, int maxResults, 
                                   double minRelevanceScore, boolean includeContent, String userId) {
        this.query = Objects.requireNonNull(query, "Query cannot be null");
        this.categories = categories != null ? List.copyOf(categories) : List.of();
        this.tags = tags != null ? List.copyOf(tags) : List.of();
        this.types = types != null ? List.copyOf(types) : List.of();
        this.maxResults = maxResults > 0 ? maxResults : 10;
        this.minRelevanceScore = Math.max(0.0, Math.min(1.0, minRelevanceScore));
        this.includeContent = includeContent;
        this.userId = userId;
    }
    
    public String getQuery() { return query; }
    public List<String> getCategories() { return categories; }
    public List<String> getTags() { return tags; }
    public List<KnowledgeType> getTypes() { return types; }
    public int getMaxResults() { return maxResults; }
    public double getMinRelevanceScore() { return minRelevanceScore; }
    public boolean isIncludeContent() { return includeContent; }
    public String getUserId() { return userId; }
    
    public boolean hasFilters() {
        return !categories.isEmpty() || !tags.isEmpty() || !types.isEmpty();
    }
    
    @Override
    public String toString() {
        return "SearchKnowledgeBaseQuery{" +
                "query='" + query + '\'' +
                ", categories=" + categories +
                ", tags=" + tags +
                ", types=" + types +
                ", maxResults=" + maxResults +
                ", minRelevanceScore=" + minRelevanceScore +
                ", includeContent=" + includeContent +
                ", userId='" + userId + '\'' +
                '}';
    }
}