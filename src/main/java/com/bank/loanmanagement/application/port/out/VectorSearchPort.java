package com.bank.loanmanagement.application.port.out;

import com.bank.loanmanagement.domain.knowledge.BankingKnowledge;

import java.util.List;
import java.util.Map;

public interface VectorSearchPort {
    
    List<BankingKnowledge> searchSimilar(List<Float> queryEmbedding, int maxResults);
    
    List<BankingKnowledge> searchSimilar(List<Float> queryEmbedding, int maxResults, 
                                        Map<String, Object> filters);
    
    void storeVector(String id, List<Float> embedding, Map<String, Object> metadata);
    
    void storeVectors(Map<String, List<Float>> embeddings, Map<String, Map<String, Object>> metadata);
    
    void updateVector(String id, List<Float> embedding, Map<String, Object> metadata);
    
    void deleteVector(String id);
    
    boolean vectorExists(String id);
    
    long getVectorCount();
    
    boolean isHealthy();
    
    void createCollection(String collectionName, int dimensions);
    
    void deleteCollection(String collectionName);
    
    List<String> listCollections();
}