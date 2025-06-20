package com.bank.loanmanagement.domain.knowledge;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class BankingKnowledge {
    
    private final String id;
    private final String title;
    private final String content;
    private final String category;
    private final List<String> tags;
    private final KnowledgeType type;
    private final double relevanceScore;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String source;
    
    public BankingKnowledge(String id, String title, String content, String category, 
                           List<String> tags, KnowledgeType type, double relevanceScore,
                           LocalDateTime createdAt, LocalDateTime updatedAt, String source) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.tags = List.copyOf(Objects.requireNonNull(tags, "Tags cannot be null"));
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.relevanceScore = relevanceScore;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        this.source = Objects.requireNonNull(source, "Source cannot be null");
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public KnowledgeType getType() { return type; }
    public double getRelevanceScore() { return relevanceScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getSource() { return source; }
    
    public boolean isRelevant(double threshold) {
        return relevanceScore >= threshold;
    }
    
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    
    public boolean isCategory(String category) {
        return this.category.equalsIgnoreCase(category);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankingKnowledge that = (BankingKnowledge) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "BankingKnowledge{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", type=" + type +
                ", relevanceScore=" + relevanceScore +
                '}';
    }
}