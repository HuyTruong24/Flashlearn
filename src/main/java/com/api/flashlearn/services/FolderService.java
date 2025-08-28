package com.api.flashlearn.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.CreateFolderRequest;
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

    public List<FolderDto> getFoldersBy(Long userId, String folderName) {
        var folders = folderRepository
                        .findFoldersByUserId(userId)
                        .orElseThrow(() -> new FolderNotFoundException("No folders found for userId: " + userId));
    
        return folders.stream()
            .filter(folder -> folder.getName().contains(folderName) || folderName.contains(folder.getName()))
            .map(folderMapper::toDto)
            .toList();
    }
    public List<FolderDto> getFavoriteFoldersBy(Long userId, String folderName) {
        var folders = folderRepository
                        .findFoldersByUserId(userId)
                        .orElseThrow(() -> new FolderNotFoundException("No folders found for userId: " + userId));
    
        return folders.stream()
            .filter(folder -> folder.isFavorite() || folder.getName().contains(folderName) || folderName.contains(folder.getName()))
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
    public boolean isFolderNameUnique(String name, Long userId) {
        var folders = folderRepository
                        .findFoldersByUserId(userId)
                        .orElseThrow(() -> new FolderNotFoundException("No folders found for userId: " + userId));
        for (Folder folder : folders) {
            if (folder.getName().equals(name)) {
                return false; // Name is not unique
            }
        }
        return true;
    }

    public FolderDto createFolder(CreateFolderRequest request, Long userId) {
        var folder = folderMapper.toEntity(request, userId);
        folderRepository.save(folder);
        return folderMapper.toDto(folder);
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

    public List<FlashcardDto> updateFlashcardsInFolder(Long folderId, List<FlashcardDto> existingCards, List<FlashcardDto> removedCards) {
        var folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new FolderNotFoundException("Folder not found with ID: " + folderId));

        if(removedCards != null) {
            flashcardRepository.deleteAllById(removedCards.stream().map(FlashcardDto::getId).toList());
        }

        if(existingCards == null) {
            return List.of();
        }
        
        List<Flashcard> flashcards = existingCards.stream()
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
    public void updateFavoriteStatus(Long folderId, boolean isFavorite) {
        var folder = folderRepository.findById(folderId).orElseThrow(
            () -> new FolderNotFoundException("Folder not found with ID: " + folderId)
        );
        folder.setFavorite(isFavorite);
        folderRepository.save(folder);
    }
}
