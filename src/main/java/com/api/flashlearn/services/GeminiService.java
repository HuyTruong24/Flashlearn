package com.api.flashlearn.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import com.api.flashlearn.dtos.FlashcardDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.lang.reflect.Type;

@Service
public class GeminiService {
    @Autowired
    private WebClient webClient;
    
    /**
     * Generates a summary of the provided content using the Gemini API.
     *
     * @param content the content to summarize
     * @return the generated summary
     */
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

    /**
     * Generates flashcards based on the provided content using the Gemini API.
     *
     * @param content the content to generate flashcards from
     * @return a list of generated flashcards
     */
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

    /**
     * Builds the request body for the Gemini API.
     *
     * @param requestText the text to include in the request
     * @return the JSON string representing the request body
     */
    private String buildRequestBody(String requestText) {
        /*  Structure of payload:
            {
                "contents": [
                    {
                        "role": "user",
                        "parts": [
                            {
                                "text": requestText
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 1
                }
            }
        */
        Gson gson = new Gson();

        JsonObject partsFirst = new JsonObject();
        partsFirst.addProperty("text", requestText);

        JsonArray parts = new JsonArray();
        parts.add(partsFirst);

        JsonObject firstElement = new JsonObject();
        firstElement.addProperty("role", "user");
        firstElement.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(firstElement);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 1);

        JsonObject jsonPayload = new JsonObject();
        jsonPayload.add("contents", contents);
        jsonPayload.add("generationConfig", generationConfig);

        return gson.toJson(jsonPayload);
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
