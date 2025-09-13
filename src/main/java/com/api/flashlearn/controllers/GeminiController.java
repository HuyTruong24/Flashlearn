package com.api.flashlearn.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.flashlearn.dtos.ErrorDto;
import com.api.flashlearn.dtos.GeminiRequest;
import com.api.flashlearn.dtos.SummaryDto;
import com.api.flashlearn.services.GeminiService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/gemini")
@RequiredArgsConstructor
public class GeminiController {
    private final GeminiService geminiService;
    /**
     * Endpoint to create a summary from the provided content.
     * @param request GeminiRequest containing the content to summarize
     * @return ResponseEntity with SummaryDto or ErrorDto
     */
    @PostMapping("/summary")
    public ResponseEntity<?> createSummary(@RequestBody GeminiRequest request) {
        var summary = geminiService.generateSummary(request.getContent());
        if (summary == null) {
            return ResponseEntity.badRequest().body(new ErrorDto("Failed to generate summary"));
        }
        return ResponseEntity.ok().body(new SummaryDto(summary));
    }
    /**
     * Endpoint to create flashcards from the provided content.
     * @param request GeminiRequest containing the content to generate flashcards from
     * @return ResponseEntity with list of flashcard DTOs or ErrorDto
     */
    @PostMapping("/flashcards")
    public ResponseEntity<?> createFlashcards(@RequestBody GeminiRequest request) {
        var flashcardDtos = geminiService.generateFlashcards(request.getContent());
        if (flashcardDtos == null) {
            return ResponseEntity.badRequest().body(new ErrorDto("Failed to generate flashcards"));
        }
        return ResponseEntity.ok().body(flashcardDtos);
    }
    
}
