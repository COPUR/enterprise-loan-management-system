package com.bank.loanmanagement.infrastructure.ai.adapter;

import com.bank.loanmanagement.application.port.out.VectorSearchPort;
import com.bank.loanmanagement.domain.knowledge.BankingKnowledge;
import com.bank.loanmanagement.domain.knowledge.KnowledgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChromaVectorStoreAdapter implements VectorSearchPort {
    
    private static final Logger logger = LoggerFactory.getLogger(ChromaVectorStoreAdapter.class);
    
    private final VectorStore vectorStore;
    
    public ChromaVectorStoreAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    @Override
    public List<BankingKnowledge> searchSimilar(List<Float> queryEmbedding, int maxResults) {
        return searchSimilar(queryEmbedding, maxResults, Map.of());
    }
    
    @Override
    public List<BankingKnowledge> searchSimilar(List<Float> queryEmbedding, int maxResults, Map<String, Object> filters) {
        try {
            SearchRequest.Builder searchRequestBuilder = SearchRequest.query("")
                    .withTopK(maxResults)
                    .withSimilarityThreshold(0.0);
            
            // Add filters if provided
            if (!filters.isEmpty()) {
                searchRequestBuilder.withFilterExpression(buildFilterExpression(filters));
            }
            
            SearchRequest searchRequest = searchRequestBuilder.build();
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            return documents.stream()
                    .map(this::documentToBankingKnowledge)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error performing vector search", e);
            return List.of();
        }
    }
    
    @Override
    public void storeVector(String id, List<Float> embedding, Map<String, Object> metadata) {
        try {
            Document document = new Document(id, metadata.get("content").toString(), metadata);
            vectorStore.add(List.of(document));
            logger.debug("Stored vector for document: {}", id);
        } catch (Exception e) {
            logger.error("Error storing vector for document: {}", id, e);
        }
    }
    
    @Override
    public void storeVectors(Map<String, List<Float>> embeddings, Map<String, Map<String, Object>> metadata) {
        try {
            List<Document> documents = embeddings.entrySet().stream()
                    .map(entry -> {
                        String id = entry.getKey();
                        Map<String, Object> docMetadata = metadata.getOrDefault(id, Map.of());
                        String content = docMetadata.getOrDefault("content", "").toString();
                        return new Document(id, content, docMetadata);
                    })
                    .collect(Collectors.toList());
            
            vectorStore.add(documents);
            logger.info("Stored {} vectors in batch", documents.size());
        } catch (Exception e) {
            logger.error("Error storing vectors in batch", e);
        }
    }
    
    @Override
    public void updateVector(String id, List<Float> embedding, Map<String, Object> metadata) {
        try {
            // ChromaDB typically handles updates by re-adding with same ID
            storeVector(id, embedding, metadata);
        } catch (Exception e) {
            logger.error("Error updating vector for document: {}", id, e);
        }
    }
    
    @Override
    public void deleteVector(String id) {
        try {
            vectorStore.delete(List.of(id));
            logger.debug("Deleted vector for document: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting vector for document: {}", id, e);
        }
    }
    
    @Override
    public boolean vectorExists(String id) {
        try {
            // Try to search for the specific document
            SearchRequest searchRequest = SearchRequest.query("")
                    .withTopK(1)
                    .withFilterExpression("id == '" + id + "'");
            
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            return !results.isEmpty();
        } catch (Exception e) {
            logger.error("Error checking vector existence for document: {}", id, e);
            return false;
        }
    }
    
    @Override
    public long getVectorCount() {
        try {
            // This is an approximation - Chroma doesn't provide direct count
            SearchRequest searchRequest = SearchRequest.query("").withTopK(Integer.MAX_VALUE);
            List<Document> allDocuments = vectorStore.similaritySearch(searchRequest);
            return allDocuments.size();
        } catch (Exception e) {
            logger.error("Error getting vector count", e);
            return 0;
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Perform a simple search to check health
            SearchRequest searchRequest = SearchRequest.query("test").withTopK(1);
            vectorStore.similaritySearch(searchRequest);
            return true;
        } catch (Exception e) {
            logger.warn("Vector store health check failed", e);
            return false;
        }
    }
    
    @Override
    public void createCollection(String collectionName, int dimensions) {
        logger.info("Collection creation is handled automatically by Chroma for collection: {}", collectionName);
    }
    
    @Override
    public void deleteCollection(String collectionName) {
        logger.warn("Collection deletion not implemented for Chroma adapter");
    }
    
    @Override
    public List<String> listCollections() {
        logger.info("Collection listing not implemented for Chroma adapter");
        return List.of();
    }
    
    private String buildFilterExpression(Map<String, Object> filters) {
        return filters.entrySet().stream()
                .map(entry -> String.format("%s == '%s'", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" AND "));
    }
    
    private BankingKnowledge documentToBankingKnowledge(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        
        String id = document.getId();
        String title = getMetadataString(metadata, "title", "Untitled");
        String content = document.getContent();
        String category = getMetadataString(metadata, "category", "General");
        List<String> tags = getMetadataList(metadata, "tags");
        KnowledgeType type = getKnowledgeType(metadata, "type");
        double relevanceScore = getMetadataDouble(metadata, "distance", 0.0);
        String source = getMetadataString(metadata, "source", "Unknown");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = getMetadataDateTime(metadata, "created_at", now);
        LocalDateTime updatedAt = getMetadataDateTime(metadata, "updated_at", now);
        
        return new BankingKnowledge(id, title, content, category, tags, type, 
                                   relevanceScore, createdAt, updatedAt, source);
    }
    
    private String getMetadataString(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private double getMetadataDouble(Map<String, Object> metadata, String key, double defaultValue) {
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getMetadataList(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of();
    }
    
    private KnowledgeType getKnowledgeType(Map<String, Object> metadata, String key) {
        String typeString = getMetadataString(metadata, key, "FAQ");
        try {
            return KnowledgeType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KnowledgeType.FAQ;
        }
    }
    
    private LocalDateTime getMetadataDateTime(Map<String, Object> metadata, String key, LocalDateTime defaultValue) {
        String dateString = getMetadataString(metadata, key, null);
        if (dateString != null) {
            try {
                return LocalDateTime.parse(dateString);
            } catch (Exception e) {
                logger.warn("Error parsing datetime from metadata: {}", dateString);
            }
        }
        return defaultValue;
    }
}