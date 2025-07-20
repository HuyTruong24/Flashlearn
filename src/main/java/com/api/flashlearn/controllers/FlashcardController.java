package com.api.flashlearn.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.ErrorDto;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.exceptions.FlashcardNotFoundException;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.services.FlashcardService;

import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/flashcards")
@AllArgsConstructor
public class FlashcardController {
    private final FlashcardService flashcardService;

    @GetMapping("/{folderId}")
    public List<FlashcardDto> getFlashcardsByFolderId(@PathVariable Long folderId) {
        return flashcardService.findByFolderId(folderId);
    }

    @DeleteMapping("/{flashcardId}")
    public ResponseEntity<Void> deleteFlashcard(@PathVariable String flashcardId) {
        flashcardService.deleteFlashcard(flashcardId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(FlashcardNotFoundException.class)
    public ResponseEntity<ErrorDto> handleFlashcardNotFound(FlashcardNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ex.getMessage()));
    }
    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<ErrorDto> handleFolderNotFound(FolderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ex.getMessage()));
    }
    
    
}
