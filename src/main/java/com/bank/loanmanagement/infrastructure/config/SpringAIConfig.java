package com.bank.loanmanagement.infrastructure.config;

import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI Configuration for Enterprise Loan Management System
 * Configures OpenAI integration with MCP support for banking AI services
 */
@Configuration
@Profile("!test")
public class SpringAIConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openAiBaseUrl;

    @Value("${spring.ai.openai.chat.options.model:gpt-4}")
    private String defaultModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;

    /**
     * OpenAI API Configuration Bean
     */
    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(openAiBaseUrl, openAiApiKey);
    }

    /**
     * OpenAI Chat Client with Banking-Optimized Settings
     */
    @Bean
    public OpenAiChatClient openAiChatClient(OpenAiApi openAiApi) {
        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .withModel(defaultModel)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build();

        return new OpenAiChatClient(openAiApi, defaultOptions);
    }

    /**
     * Loan Analysis Chat Client - Specialized for loan application analysis
     */
    @Bean
    public OpenAiChatClient loanAnalysisChatClient(OpenAiApi openAiApi) {
        OpenAiChatOptions loanOptions = OpenAiChatOptions.builder()
                .withModel("gpt-4")
                .withTemperature(0.2) // Lower temperature for consistent analysis
                .withMaxTokens(1500)
                .build();

        return new OpenAiChatClient(openAiApi, loanOptions);
    }

    /**
     * Risk Assessment Chat Client - Specialized for credit risk analysis
     */
    @Bean
    public OpenAiChatClient riskAssessmentChatClient(OpenAiApi openAiApi) {
        OpenAiChatOptions riskOptions = OpenAiChatOptions.builder()
                .withModel("gpt-4")
                .withTemperature(0.1) // Very low temperature for risk assessment
                .withMaxTokens(1200)
                .build();

        return new OpenAiChatClient(openAiApi, riskOptions);
    }

    /**
     * Customer Service Chat Client - For customer interactions and recommendations
     */
    @Bean
    public OpenAiChatClient customerServiceChatClient(OpenAiApi openAiApi) {
        OpenAiChatOptions customerOptions = OpenAiChatOptions.builder()
                .withModel("gpt-4")
                .withTemperature(0.4) // Slightly higher for more natural responses
                .withMaxTokens(800)
                .build();

        return new OpenAiChatClient(openAiApi, customerOptions);
    }
}