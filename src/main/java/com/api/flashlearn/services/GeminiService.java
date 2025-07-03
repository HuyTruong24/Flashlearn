package com.api.flashlearn.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.api.flashlearn.dtos.FlashcardDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;


import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.lang.reflect.Type;

@Service
public class GeminiService {
    private static final String GEMINI_API_KEY = "AIzaSyCAEXz23sE2nv3oSgMQ2CnsAPzg9_MaVa8";
    private WebClient webClient;
    public GeminiService() {
        //webClient = WebClient.create(String.format("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s", GEMINI_API_KEY))
        this.webClient = WebClient.builder()
                .baseUrl(String.format("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s", GEMINI_API_KEY))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    public String generateSummary(String content) {
        String requestText = String.format("Summarize the following text please:\n---------------------------\n%s", content);
        
        String response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(requestText))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return getText(response);
    }

    public List<FlashcardDto> generateFlashcards(String content) {
        String command = "Generate concise short answer questions based on the provided overall summary of the file. Focus on definitions, key concepts, and the relationships between them. Each flashcard should be suitable for quick review and should reference the overall lecture content. Do not invent any extra concept that is not covered in the summary. The JSON should be an array of flashcards, each with an flashcardID, question, and answer.\n\nContent:\n";
        String requestText = String.format("%s%s", command, content);
        String response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(requestText))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String text = getText(response);
        return convertToFlashCards(text);
        
    }
    private String buildRequestBody(String requestText) {
        return "{"
                + "\"contents\": [{\"role\": \"user\", \"parts\": [{\"text\": \"" + requestText + "\"}]}],"
                + "\"generationConfig\": {\"temperature\": 1}"
                + "}";
    }
    private String getText(String rb) {
        String text = null;
        if (rb == null || rb.isEmpty()) return text;

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(rb, JsonObject.class);

        // Extract the relevant text from the response
        if (jsonResponse.has("candidates") && !jsonResponse.getAsJsonArray("candidates").isEmpty()) {
            JsonObject firstCandidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
            if (firstCandidate.has("content") && firstCandidate.getAsJsonObject("content").has("parts") && !firstCandidate.getAsJsonObject("content").getAsJsonArray("parts").isEmpty()) {
                text = firstCandidate.getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();
            }
        }

        return text;
    }
    private List<FlashcardDto> convertToFlashCards(String flashcardJson){
        if(flashcardJson == null) return null;

        String cleaned = flashcardJson.replaceAll("(?s).*?\\[", "[")  // keep only JSON array
                .replaceAll("]\n```", "]");     // strip trailing markdown
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type listType = new TypeToken<List<FlashcardDto>>(){}.getType();

        return gson.fromJson(cleaned, listType);
    }
    
    
}
