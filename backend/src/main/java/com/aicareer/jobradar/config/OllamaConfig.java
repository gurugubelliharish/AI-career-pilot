package com.aicareer.jobradar.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.chat-model}")
    private String chatModel;

    @Value("${app.ollama.embed-model}")
    private String embeddingModel;

    // LangChain4j chat model for complex prompts (resume analysis, cover letter generation)
    @Bean
    public OllamaChatModel langChain4jChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModel)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    // LangChain4j embedding model for semantic similarity (job matching)
    @Bean
    public OllamaEmbeddingModel langChain4jEmbeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
