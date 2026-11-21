package com.api.flashlearn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

@Configuration
@Data
public class GeminiConfig {
    @Value("${gemini.apiKey}") // injects the api key from application.yaml
    private String apiKey;
}
