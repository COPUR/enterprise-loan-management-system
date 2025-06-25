package com.bank.loanmanagement.ai.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring AI configuration for OpenAI integration
 */
@Configuration
@EnableRetry
public class SpringAiConfiguration {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4}")
    private String chatModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.3}")
    private Float temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private Integer maxTokens;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(openAiApiKey);
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi, OpenAiChatOptions.builder()
                .withModel(chatModel)
                .withTemperature(temperature)
                .withMaxTokens(maxTokens)
                .build());
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel(this.chatModel)
                        .withTemperature(temperature)
                        .withMaxTokens(maxTokens)
                        .build())
                .build();
    }
}