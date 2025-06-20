package com.bank.loanmanagement.domain.knowledge;

import java.util.List;
import java.util.Objects;

public class KnowledgeQuery {
    
    private final String queryText;
    private final List<String> categories;
    private final List<String> tags;
    private final List<KnowledgeType> types;
    private final int maxResults;
    private final double minRelevanceScore;
    private final boolean includeContent;
    
    private KnowledgeQuery(Builder builder) {
        this.queryText = Objects.requireNonNull(builder.queryText, "Query text cannot be null");
        this.categories = List.copyOf(builder.categories);
        this.tags = List.copyOf(builder.tags);
        this.types = List.copyOf(builder.types);
        this.maxResults = builder.maxResults;
        this.minRelevanceScore = builder.minRelevanceScore;
        this.includeContent = builder.includeContent;
    }
    
    public String getQueryText() { return queryText; }
    public List<String> getCategories() { return categories; }
    public List<String> getTags() { return tags; }
    public List<KnowledgeType> getTypes() { return types; }
    public int getMaxResults() { return maxResults; }
    public double getMinRelevanceScore() { return minRelevanceScore; }
    public boolean isIncludeContent() { return includeContent; }
    
    public boolean hasFilters() {
        return !categories.isEmpty() || !tags.isEmpty() || !types.isEmpty();
    }
    
    public static Builder builder(String queryText) {
        return new Builder(queryText);
    }
    
    public static class Builder {
        private final String queryText;
        private List<String> categories = List.of();
        private List<String> tags = List.of();
        private List<KnowledgeType> types = List.of();
        private int maxResults = 10;
        private double minRelevanceScore = 0.0;
        private boolean includeContent = true;
        
        private Builder(String queryText) {
            this.queryText = queryText;
        }
        
        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }
        
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public Builder types(List<KnowledgeType> types) {
            this.types = types;
            return this;
        }
        
        public Builder maxResults(int maxResults) {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("Max results must be positive");
            }
            this.maxResults = maxResults;
            return this;
        }
        
        public Builder minRelevanceScore(double minRelevanceScore) {
            if (minRelevanceScore < 0.0 || minRelevanceScore > 1.0) {
                throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
            }
            this.minRelevanceScore = minRelevanceScore;
            return this;
        }
        
        public Builder includeContent(boolean includeContent) {
            this.includeContent = includeContent;
            return this;
        }
        
        public KnowledgeQuery build() {
            return new KnowledgeQuery(this);
        }
    }
    
    @Override
    public String toString() {
        return "KnowledgeQuery{" +
                "queryText='" + queryText + '\'' +
                ", categories=" + categories +
                ", tags=" + tags +
                ", types=" + types +
                ", maxResults=" + maxResults +
                ", minRelevanceScore=" + minRelevanceScore +
                ", includeContent=" + includeContent +
                '}';
    }
}