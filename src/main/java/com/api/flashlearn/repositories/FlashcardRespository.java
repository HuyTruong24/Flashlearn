package com.api.flashlearn.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.flashlearn.entities.Flashcard;

public interface FlashcardRespository extends JpaRepository<Flashcard, String> {
    List<Flashcard> findByFolderId(Long folderId);
    
}
