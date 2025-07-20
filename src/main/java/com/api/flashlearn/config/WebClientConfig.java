package com.api.flashlearn.config;

import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
@AllArgsConstructor
public class WebClientConfig {
    private final GeminiConfig geminiConfig;
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(String.format("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s", geminiConfig.getApiKey()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
