package com.bank.loanmanagement.infrastructure.ai.config;

import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.vectorstore.chroma.enabled", havingValue = "true", matchIfMissing = true)
public class RAGConfiguration {
    
    @Value("${spring.ai.vectorstore.chroma.host:localhost}")
    private String chromaHost;
    
    @Value("${spring.ai.vectorstore.chroma.port:8000}")
    private int chromaPort;
    
    @Value("${spring.ai.vectorstore.chroma.collection-name:banking_knowledge}")
    private String collectionName;
    
    @Value("${spring.ai.embedding.model:text-embedding-ada-002}")
    private String embeddingModel;
    
    @Bean
    public ChromaApi chromaApi() {
        return new ChromaApi(String.format("http://%s:%d", chromaHost, chromaPort));
    }
    
    @Bean
    public EmbeddingClient embeddingClient(OpenAiApi openAiApi) {
        return new OpenAiEmbeddingClient(openAiApi)
                .withDefaultOptions(OpenAiEmbeddingClient.OpenAiEmbeddingOptions.builder()
                        .withModel(embeddingModel)
                        .build());
    }
    
    @Bean
    public VectorStore vectorStore(ChromaApi chromaApi, EmbeddingClient embeddingClient) {
        return new ChromaVectorStore(chromaApi, embeddingClient, collectionName, true);
    }
}