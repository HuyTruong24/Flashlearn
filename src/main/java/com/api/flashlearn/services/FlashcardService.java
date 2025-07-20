package com.api.flashlearn.services;

import java.util.List;


import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.entities.Flashcard;
import com.api.flashlearn.exceptions.FlashcardNotFoundException;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.mappers.FlashcardMapper;
import com.api.flashlearn.repositories.FlashcardRespository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FlashcardService {
    private final FlashcardRespository flashcardRepository;
    private final FlashcardMapper flashcardMapper;

    public List<FlashcardDto> findByFolderId(Long folderId) { 
        List<Flashcard> flashcards = flashcardRepository.findByFolderId(folderId);
        if (flashcards.isEmpty()) {
            throw new FolderNotFoundException("No flashcards found for folder ID: " + folderId);
        }
        return flashcards.stream().map(flashcardMapper::toDto).toList();
    }
    
    public void deleteFlashcard(String flashcardId) {
         var flashcard = flashcardRepository.findById(flashcardId).orElseThrow(
            () -> new FlashcardNotFoundException("Flashcard not found with ID: " + flashcardId)
        );
        flashcardRepository.delete(flashcard);
    }
}
