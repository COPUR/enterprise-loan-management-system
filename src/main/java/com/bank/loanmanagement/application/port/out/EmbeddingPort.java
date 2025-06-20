package com.bank.loanmanagement.application.port.out;

import java.util.List;

public interface EmbeddingPort {
    
    List<Float> generateEmbedding(String text);
    
    List<List<Float>> generateEmbeddings(List<String> texts);
    
    double calculateSimilarity(List<Float> embedding1, List<Float> embedding2);
    
    List<Float> combineEmbeddings(List<List<Float>> embeddings);
    
    boolean isHealthy();
    
    int getEmbeddingDimensions();
    
    String getModelName();
}