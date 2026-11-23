package com.api.flashlearn.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

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
import com.api.flashlearn.services.FolderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FolderServiceTest {

    @Mock
    private FolderRespository folderRepository;

    @Mock
    private FolderMapper folderMapper;

    @Mock
    private FlashcardRespository flashcardRepository;

    @Mock
    private FlashcardMapper flashcardMapper;

    @InjectMocks
    private FolderService folderService;

    // getFoldersBy

    @Test
    void getFoldersBy_shouldReturnFoldersMatchingEitherContainmentDirection() {
        // Arrange
        Long userId = 7L;
        String search = "Math";
        Folder nameContainsSearch = mock(Folder.class);
        Folder searchContainsName = mock(Folder.class);
        Folder noMatch = mock(Folder.class);
        FolderDto firstDto = mock(FolderDto.class);
        FolderDto secondDto = mock(FolderDto.class);

        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of(nameContainsSearch, searchContainsName, noMatch)));
        when(nameContainsSearch.getName()).thenReturn("Advanced Math");
        when(searchContainsName.getName()).thenReturn("Mat");
        when(noMatch.getName()).thenReturn("History");
        when(folderMapper.toDto(nameContainsSearch)).thenReturn(firstDto);
        when(folderMapper.toDto(searchContainsName)).thenReturn(secondDto);

        // Act
        List<FolderDto> result = folderService.getFoldersBy(userId, search);

        // Assert
        assertEquals(List.of(firstDto, secondDto), result);
        verify(folderMapper).toDto(nameContainsSearch);
        verify(folderMapper).toDto(searchContainsName);
        verify(folderMapper, never()).toDto(noMatch);
    }

    @Test
    void getFoldersBy_shouldReturnEmptyListWhenNoFoldersMatch() {
        // Arrange
        Long userId = 7L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of(folder)));
        when(folder.getName()).thenReturn("History");

        // Act
        List<FolderDto> result = folderService.getFoldersBy(userId, "Math");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(folderMapper);
    }

    @Test
    void getFoldersBy_shouldThrowWhenUserHasNoFolderCollection() {
        // Arrange
        Long userId = 7L;
        when(folderRepository.findFoldersByUserId(userId)).thenReturn(Optional.empty());

        // Act
        FolderNotFoundException exception = assertThrows(
                FolderNotFoundException.class,
                () -> folderService.getFoldersBy(userId, "Math"));

        // Assert
        assertEquals("No folders found for userId: " + userId, exception.getMessage());
        verifyNoInteractions(folderMapper);
    }

    // getFavoriteFoldersBy

    @Test
    void getFavoriteFoldersBy_shouldIncludeFavoritesAndNameMatches() {
        // Arrange
        Long userId = 7L;
        String search = "Math";
        Folder favorite = mock(Folder.class);
        Folder nameMatch = mock(Folder.class);
        Folder searchContainsName = mock(Folder.class);
        Folder excluded = mock(Folder.class);
        FolderDto favoriteDto = mock(FolderDto.class);
        FolderDto matchDto = mock(FolderDto.class);
        FolderDto partialDto = mock(FolderDto.class);

        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of(favorite, nameMatch, searchContainsName, excluded)));
        when(favorite.isFavorite()).thenReturn(true);
        when(nameMatch.isFavorite()).thenReturn(false);
        when(nameMatch.getName()).thenReturn("Mathematics");
        when(searchContainsName.isFavorite()).thenReturn(false);
        when(searchContainsName.getName()).thenReturn("Mat");
        when(excluded.isFavorite()).thenReturn(false);
        when(excluded.getName()).thenReturn("History");

        when(folderMapper.toDto(favorite)).thenReturn(favoriteDto);
        when(folderMapper.toDto(nameMatch)).thenReturn(matchDto);
        when(folderMapper.toDto(searchContainsName)).thenReturn(partialDto);

        // Act
        List<FolderDto> result = folderService.getFavoriteFoldersBy(userId, search);

        // Assert
        assertEquals(List.of(favoriteDto, matchDto, partialDto), result);
        verify(folderMapper, never()).toDto(excluded);
    }

    @Test
    void getFavoriteFoldersBy_shouldReturnEmptyListWhenNothingMatches() {
        // Arrange
        Long userId = 7L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of(folder)));
        when(folder.isFavorite()).thenReturn(false);
        when(folder.getName()).thenReturn("History");

        // Act
        List<FolderDto> result = folderService.getFavoriteFoldersBy(userId, "Math");

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(folderMapper);
    }

    @Test
    void getFavoriteFoldersBy_shouldThrowWhenUserHasNoFolderCollection() {
        // Arrange
        Long userId = 7L;
        when(folderRepository.findFoldersByUserId(userId)).thenReturn(Optional.empty());

        // Act
        FolderNotFoundException exception = assertThrows(
                FolderNotFoundException.class,
                () -> folderService.getFavoriteFoldersBy(userId, "Math"));

        // Assert
        assertEquals("No folders found for userId: " + userId, exception.getMessage());
        verifyNoInteractions(folderMapper);
    }

    // getFolerById

    @Test
    void getFolerById_shouldReturnMappedFolder() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        FolderDto expected = mock(FolderDto.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(folderMapper.toDto(folder)).thenReturn(expected);

        // Act
        FolderDto result = folderService.getFolerById(folderId);

        // Assert
        assertSame(expected, result);
        verify(folderMapper).toDto(folder);
    }

    @Test
    void getFolerById_shouldThrowWhenFolderDoesNotExist() {
        // Arrange
        Long folderId = 3L;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // Act
        FolderNotFoundException exception = assertThrows(
                FolderNotFoundException.class,
                () -> folderService.getFolerById(folderId));

        // Assert
        assertEquals("Folder not found with ID: " + folderId, exception.getMessage());
        verifyNoInteractions(folderMapper);
    }

    // getFolerItems

    @Test
    void getFolerItems_shouldReturnMappedFolderItems() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        FolderItemDto expected = mock(FolderItemDto.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(folderMapper.toFolderItemDto(folder)).thenReturn(expected);

        // Act
        FolderItemDto result = folderService.getFolerItems(folderId);

        // Assert
        assertSame(expected, result);
        verify(folderMapper).toFolderItemDto(folder);
    }

    @Test
    void getFolerItems_shouldThrowWhenFolderDoesNotExist() {
        // Arrange
        Long folderId = 3L;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // Act
        FolderNotFoundException exception = assertThrows(
                FolderNotFoundException.class,
                () -> folderService.getFolerItems(folderId));

        // Assert
        assertEquals("Folder not found with ID: " + folderId, exception.getMessage());
        verifyNoInteractions(folderMapper);
    }

    // isFolderNameUnique

    @Test
    void isFolderNameUnique_shouldReturnFalseWhenNameAlreadyExists() {
        // Arrange
        Long userId = 7L;
        String name = "Biology";
        Folder folder = mock(Folder.class);
        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of(folder)));
        when(folder.getName()).thenReturn(name);

        // Act
        boolean result = folderService.isFolderNameUnique(name, userId);

        // Assert
        assertFalse(result);
    }

    @Test
    void isFolderNameUnique_shouldReturnTrueWhenNameDoesNotExist() {
        // Arrange
        Long userId = 7L;
        Folder first = mock(Folder.class);
        Folder second = mock(Folder.class);
        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of(first, second)));
        when(first.getName()).thenReturn("Biology");
        when(second.getName()).thenReturn("History");

        // Act
        boolean result = folderService.isFolderNameUnique("Math", userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isFolderNameUnique_shouldReturnTrueWhenFolderListIsEmpty() {
        // Arrange
        Long userId = 7L;
        when(folderRepository.findFoldersByUserId(userId))
                .thenReturn(Optional.of(List.of()));

        // Act
        boolean result = folderService.isFolderNameUnique("Math", userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isFolderNameUnique_shouldThrowWhenFolderCollectionIsMissing() {
        // Arrange
        Long userId = 7L;
        when(folderRepository.findFoldersByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FolderNotFoundException.class,
                () -> folderService.isFolderNameUnique("Math", userId));
    }

    // createFolder

    @Test
    void createFolder_shouldMapSaveAndReturnFolderDto() {
        // Arrange
        Long userId = 7L;
        CreateFolderRequest request = mock(CreateFolderRequest.class);
        Folder folder = mock(Folder.class);
        FolderDto expected = mock(FolderDto.class);
        when(folderMapper.toEntity(request, userId)).thenReturn(folder);
        when(folderMapper.toDto(folder)).thenReturn(expected);

        // Act
        FolderDto result = folderService.createFolder(request, userId);

        // Assert
        assertSame(expected, result);
        verify(folderMapper).toEntity(request, userId);
        verify(folderRepository).save(folder);
        verify(folderMapper).toDto(folder);
    }

    // addToFolder

    @Test
    void addToFolder_shouldMapAttachSaveAndReturnFlashcards() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        CreateFlashcardRequest request1 = mock(CreateFlashcardRequest.class);
        CreateFlashcardRequest request2 = mock(CreateFlashcardRequest.class);
        Flashcard card1 = mock(Flashcard.class);
        Flashcard card2 = mock(Flashcard.class);
        FlashcardDto dto1 = mock(FlashcardDto.class);
        FlashcardDto dto2 = mock(FlashcardDto.class);

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(flashcardMapper.toEntity(request1)).thenReturn(card1);
        when(flashcardMapper.toEntity(request2)).thenReturn(card2);
        when(flashcardMapper.toDto(card1)).thenReturn(dto1);
        when(flashcardMapper.toDto(card2)).thenReturn(dto2);

        // Act
        List<FlashcardDto> result = folderService.addToFolder(
                folderId, List.of(request1, request2));

        // Assert
        assertEquals(List.of(dto1, dto2), result);
        verify(card1).setFolder(folder);
        verify(card2).setFolder(folder);
        verify(flashcardRepository).saveAll(List.of(card1, card2));
    }

    @Test
    void addToFolder_shouldSaveEmptyListWhenNoCardsProvided() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        // Act
        List<FlashcardDto> result = folderService.addToFolder(folderId, List.of());

        // Assert
        assertTrue(result.isEmpty());
        verify(flashcardRepository).saveAll(List.of());
        verifyNoInteractions(flashcardMapper);
    }

    @Test
    void addToFolder_shouldThrowWhenFolderDoesNotExist() {
        // Arrange
        Long folderId = 3L;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FolderNotFoundException.class,
                () -> folderService.addToFolder(folderId, List.of()));
        verifyNoInteractions(flashcardMapper, flashcardRepository);
    }

    // updateFlashcardsInFolder

    @Test
    void updateFlashcardsInFolder_shouldDeleteRemovedCardsAndSaveExistingCards() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        FlashcardDto removed1 = mock(FlashcardDto.class);
        FlashcardDto removed2 = mock(FlashcardDto.class);
        FlashcardDto existing1 = mock(FlashcardDto.class);
        FlashcardDto existing2 = mock(FlashcardDto.class);
        Flashcard card1 = mock(Flashcard.class);
        Flashcard card2 = mock(Flashcard.class);
        FlashcardDto resultDto1 = mock(FlashcardDto.class);
        FlashcardDto resultDto2 = mock(FlashcardDto.class);

        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(removed1.getId()).thenReturn("removed-1");
        when(removed2.getId()).thenReturn("removed-2");
        when(flashcardMapper.toEntity(existing1)).thenReturn(card1);
        when(flashcardMapper.toEntity(existing2)).thenReturn(card2);
        when(flashcardMapper.toDto(card1)).thenReturn(resultDto1);
        when(flashcardMapper.toDto(card2)).thenReturn(resultDto2);

        // Act
        List<FlashcardDto> result = folderService.updateFlashcardsInFolder(
                folderId,
                List.of(existing1, existing2),
                List.of(removed1, removed2));

        // Assert
        assertEquals(List.of(resultDto1, resultDto2), result);
        verify(flashcardRepository).deleteAllById(List.of("removed-1", "removed-2"));
        verify(card1).setFolder(folder);
        verify(card2).setFolder(folder);
        verify(flashcardRepository).saveAll(List.of(card1, card2));
    }

    @Test
    void updateFlashcardsInFolder_shouldSkipDeletionWhenRemovedCardsIsNull() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        // Act
        List<FlashcardDto> result = folderService.updateFlashcardsInFolder(
                folderId, null, null);

        // Assert
        assertTrue(result.isEmpty());
        verify(flashcardRepository, never()).deleteAllById(anyList());
        verify(flashcardRepository, never()).saveAll(anyList());
        verifyNoInteractions(flashcardMapper);
    }

    @Test
    void updateFlashcardsInFolder_shouldDeleteRemovedCardsAndReturnEmptyWhenExistingCardsIsNull() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        FlashcardDto removed = mock(FlashcardDto.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(removed.getId()).thenReturn("removed-1");

        // Act
        List<FlashcardDto> result = folderService.updateFlashcardsInFolder(
                folderId, null, List.of(removed));

        // Assert
        assertTrue(result.isEmpty());
        verify(flashcardRepository).deleteAllById(List.of("removed-1"));
        verify(flashcardRepository, never()).saveAll(anyList());
        verifyNoInteractions(flashcardMapper);
    }

    @Test
    void updateFlashcardsInFolder_shouldSaveEmptyListWhenExistingCardsIsEmpty() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        // Act
        List<FlashcardDto> result = folderService.updateFlashcardsInFolder(
                folderId, List.of(), List.of());

        // Assert
        assertTrue(result.isEmpty());
        verify(flashcardRepository).deleteAllById(List.of());
        verify(flashcardRepository).saveAll(List.of());
        verifyNoInteractions(flashcardMapper);
    }

    @Test
    void updateFlashcardsInFolder_shouldThrowWhenFolderDoesNotExist() {
        // Arrange
        Long folderId = 3L;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FolderNotFoundException.class,
                () -> folderService.updateFlashcardsInFolder(folderId, List.of(), List.of()));
        verifyNoInteractions(flashcardRepository, flashcardMapper);
    }

    // deleteFolder

    @Test
    void deleteFolder_shouldDeleteFolderWhenFound() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        // Act
        folderService.deleteFolder(folderId);

        // Assert
        verify(folderRepository).findById(folderId);
        verify(folderRepository).delete(folder);
    }

    @Test
    void deleteFolder_shouldThrowWhenFolderDoesNotExist() {
        // Arrange
        Long folderId = 3L;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FolderNotFoundException.class,
                () -> folderService.deleteFolder(folderId));
        verify(folderRepository, never()).delete(any(Folder.class));
    }

    // updateFavoriteStatus

    @Test
    void updateFavoriteStatus_shouldSetFavoriteTrueAndSaveFolder() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        // Act
        folderService.updateFavoriteStatus(folderId, true);

        // Assert
        verify(folder).setFavorite(true);
        verify(folderRepository).save(folder);
    }

    @Test
    void updateFavoriteStatus_shouldSetFavoriteFalseAndSaveFolder() {
        // Arrange
        Long folderId = 3L;
        Folder folder = mock(Folder.class);
        when(folderRepository.findById(folderId)).thenReturn(Optional.of(folder));

        // Act
        folderService.updateFavoriteStatus(folderId, false);

        // Assert
        verify(folder).setFavorite(false);
        verify(folderRepository).save(folder);
    }

    @Test
    void updateFavoriteStatus_shouldThrowWhenFolderDoesNotExist() {
        // Arrange
        Long folderId = 3L;
        when(folderRepository.findById(folderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FolderNotFoundException.class,
                () -> folderService.updateFavoriteStatus(folderId, true));
        verify(folderRepository, never()).save(any(Folder.class));
    }
}
