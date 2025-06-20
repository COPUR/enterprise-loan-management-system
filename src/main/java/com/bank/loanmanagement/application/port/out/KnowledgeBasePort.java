package com.bank.loanmanagement.application.port.out;

import com.bank.loanmanagement.domain.knowledge.BankingKnowledge;
import com.bank.loanmanagement.domain.knowledge.KnowledgeQuery;
import com.bank.loanmanagement.domain.knowledge.KnowledgeSearchResult;
import com.bank.loanmanagement.domain.knowledge.KnowledgeType;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBasePort {
    
    KnowledgeSearchResult searchKnowledge(KnowledgeQuery query);
    
    List<BankingKnowledge> findSimilarContent(String content, int maxResults);
    
    Optional<BankingKnowledge> findById(String id);
    
    List<BankingKnowledge> findByCategory(String category);
    
    List<BankingKnowledge> findByType(KnowledgeType type);
    
    List<BankingKnowledge> findByTags(List<String> tags);
    
    void indexKnowledge(BankingKnowledge knowledge);
    
    void indexKnowledgeBatch(List<BankingKnowledge> knowledgeList);
    
    void updateKnowledge(BankingKnowledge knowledge);
    
    void deleteKnowledge(String id);
    
    boolean isHealthy();
    
    long getKnowledgeCount();
    
    List<String> getAvailableCategories();
    
    List<String> getAvailableTags();
}