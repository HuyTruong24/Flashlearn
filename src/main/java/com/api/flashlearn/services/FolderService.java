package com.api.flashlearn.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.dtos.FolderDto;
import com.api.flashlearn.dtos.FolderItemDto;
import com.api.flashlearn.entities.Flashcard;
import com.api.flashlearn.entities.Folder;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.mappers.FlashcardMapper;
import com.api.flashlearn.mappers.FolderMapper;
import com.api.flashlearn.repositories.FlashcardRespository;
import com.api.flashlearn.repositories.FolderRespository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FolderService {
    private final FolderRespository folderRepository;
    private final FolderMapper folderMapper;
    private final FlashcardRespository flashcardRepository;
    private final FlashcardMapper flashcardMapper;

    public List<FolderDto> getFoldersByUsername(String username) {
        var folders = folderRepository.findFoldersByUsername(username);
        if (folders.isEmpty()) {
            throw new FolderNotFoundException("No folders found for username: " + username);
        }
        return folders.stream()
            .map(folderMapper::toDto)
            .toList();
    }
    public FolderDto getFolerById(Long folderId) {
        var folder = folderRepository.findById(folderId).orElseThrow(() -> new FolderNotFoundException("Folder not found with ID: " + folderId));
        return folderMapper.toDto(folder);
    }
    public FolderItemDto getFolerItems(Long folderId) {
        var folder = folderRepository.findById(folderId).orElseThrow(() -> new FolderNotFoundException("Folder not found with ID: " + folderId));
        return folderMapper.toFolderItemDto(folder);
    }

    public List<FlashcardDto> addToFolder(Long folderId, List<CreateFlashcardRequest> cards) {
        var folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new FolderNotFoundException("Folder not found with ID: " + folderId));

        List<Flashcard> flashcards = cards.stream()
            .map(card -> {
                var flashcard = flashcardMapper.toEntity(card);
                flashcard.setFolder(folder);
                return flashcard;
            })
            .toList();

        flashcardRepository.saveAll(flashcards);

        return flashcards.stream()
            .map(flashcardMapper::toDto)
            .toList();
    }

    public void deleteFolder(Long id) {
        var folder = folderRepository.findById(id).orElseThrow(
            () -> new FolderNotFoundException("Folder not found with ID: " + id)
        );
        folderRepository.delete(folder);
    }
}
