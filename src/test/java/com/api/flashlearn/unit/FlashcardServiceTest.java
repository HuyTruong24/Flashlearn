package com.api.flashlearn.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.entities.Flashcard;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.exceptions.FlashcardNotFoundException;
import com.api.flashlearn.mappers.FlashcardMapper;
import com.api.flashlearn.repositories.FlashcardRespository;
import com.api.flashlearn.services.FlashcardService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FlashcardServiceTest {

        @Mock
        private FlashcardRespository flashcardRepository;

        @Mock
        private FlashcardMapper flashcardMapper;

        @InjectMocks
        private FlashcardService flashcardService;

        @Test
        void findByFolderId_shouldReturnMappedFlashcardsWhenFound() {
                // Arrange
                Long folderId = 10L;
                Flashcard first = mock(Flashcard.class);
                Flashcard second = mock(Flashcard.class);
                FlashcardDto firstDto = mock(FlashcardDto.class);
                FlashcardDto secondDto = mock(FlashcardDto.class);

                when(flashcardRepository.findByFolderId(folderId))
                                .thenReturn(List.of(first, second));
                when(flashcardMapper.toDto(first)).thenReturn(firstDto);
                when(flashcardMapper.toDto(second)).thenReturn(secondDto);

                // Act
                List<FlashcardDto> result = flashcardService.findByFolderId(folderId);

                // Assert
                assertEquals(List.of(firstDto, secondDto), result);
                verify(flashcardRepository).findByFolderId(folderId);
                verify(flashcardMapper).toDto(first);
                verify(flashcardMapper).toDto(second);
                verifyNoMoreInteractions(flashcardRepository, flashcardMapper);
        }

        @Test
        void findByFolderId_shouldThrowFolderNotFoundWhenNoFlashcardsExist() {
                // Arrange
                Long folderId = 10L;
                when(flashcardRepository.findByFolderId(folderId)).thenReturn(List.of());

                // Act
                FolderNotFoundException exception = assertThrows(
                                FolderNotFoundException.class,
                                () -> flashcardService.findByFolderId(folderId));

                // Assert
                assertEquals("No flashcards found for folder ID: " + folderId,
                                exception.getMessage());
                verify(flashcardRepository).findByFolderId(folderId);
                verifyNoInteractions(flashcardMapper);
        }

        @Test
        void deleteFlashcard_shouldDeleteFlashcardWhenFound() {
                // Arrange
                String flashcardId = "card-1";
                Flashcard flashcard = mock(Flashcard.class);
                when(flashcardRepository.findById(flashcardId))
                                .thenReturn(Optional.of(flashcard));

                // Act
                flashcardService.deleteFlashcard(flashcardId);

                // Assert
                verify(flashcardRepository).findById(flashcardId);
                verify(flashcardRepository).delete(flashcard);
                verifyNoInteractions(flashcardMapper);
        }

        @Test
        void deleteFlashcard_shouldThrowWhenFlashcardDoesNotExist() {
                // Arrange
                String flashcardId = "missing-card";
                when(flashcardRepository.findById(flashcardId))
                                .thenReturn(Optional.empty());

                // Act
                FlashcardNotFoundException exception = assertThrows(
                                FlashcardNotFoundException.class,
                                () -> flashcardService.deleteFlashcard(flashcardId));

                // Assert
                assertEquals("Flashcard not found with ID: " + flashcardId,
                                exception.getMessage());
                verify(flashcardRepository).findById(flashcardId);
                verify(flashcardRepository, never()).delete(any(Flashcard.class));
                verifyNoInteractions(flashcardMapper);
        }
}
