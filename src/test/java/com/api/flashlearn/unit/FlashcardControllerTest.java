package com.api.flashlearn.unit;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.api.flashlearn.controllers.FlashcardController;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.exceptions.FlashcardNotFoundException;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.services.FlashcardService;
import com.api.flashlearn.services.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FlashcardController.class)
class FlashcardControllerTest {
        @Autowired
        private MockMvc mvc;
        @MockitoBean
        private FlashcardService flashcardService;
        @MockitoBean
        private JwtService jwtService;

        @Test
        void getFlashcards_returnsCards() throws Exception {
                when(flashcardService.findByFolderId(4L))
                                .thenReturn(List.of(new FlashcardDto("c1", "Question", "Answer")));
                mvc.perform(get("/flashcards/4"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value("c1"))
                                .andExpect(jsonPath("$[0].question").value("Question"))
                                .andExpect(jsonPath("$[0].answer").value("Answer"));
                verify(flashcardService).findByFolderId(4L);
        }

        @Test
        void getFlashcards_returnsEmptyArrayWhenServiceReturnsEmptyList() throws Exception {
                when(flashcardService.findByFolderId(4L)).thenReturn(List.of());
                mvc.perform(get("/flashcards/4")).andExpect(status().isOk()).andExpect(content().json("[]"));
        }

        @Test
        void getFlashcards_returns404WhenFolderHasNoCards() throws Exception {
                when(flashcardService.findByFolderId(4L))
                                .thenThrow(new FolderNotFoundException("No flashcards found"));
                mvc.perform(get("/flashcards/4"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("No flashcards found"));
        }

        @Test
        void deleteFlashcard_returns204() throws Exception {
                mvc.perform(delete("/flashcards/card-123")).andExpect(status().isNoContent());
                verify(flashcardService).deleteFlashcard("card-123");
        }

        @Test
        void deleteFlashcard_returns404WhenFlashcardDoesNotExist() throws Exception {
                doThrow(new FlashcardNotFoundException("Flashcard missing"))
                                .when(flashcardService).deleteFlashcard("missing");
                mvc.perform(delete("/flashcards/missing"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Flashcard missing"));
        }

        @Test
        void deleteFlashcard_returns404WhenFolderDoesNotExist() throws Exception {
                doThrow(new FolderNotFoundException("Folder missing"))
                                .when(flashcardService).deleteFlashcard("card-123");
                mvc.perform(delete("/flashcards/card-123"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Folder missing"));
        }
}
