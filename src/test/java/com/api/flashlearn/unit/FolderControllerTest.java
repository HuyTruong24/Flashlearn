package com.api.flashlearn.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.api.flashlearn.controllers.FolderController;
import com.api.flashlearn.dtos.*;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.exceptions.DuplicateFolderNameException;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.FolderService;
import com.api.flashlearn.services.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FolderController.class)
class FolderControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private FolderService folderService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = mock(User.class);
        when(currentUser.getId()).thenReturn(7L);
        when(authService.getCurrentUser()).thenReturn(currentUser);
    }

    private FolderDto folder(long id, String name) {
        var dto = new FolderDto();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }

    @Test
    void getFolderById_returnsFolder() throws Exception {
        when(folderService.getFolerById(3L)).thenReturn(folder(3L, "Biology"));
        mvc.perform(get("/folders/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Biology"));
    }

    @Test
    void getFolderById_returns404WhenMissing() throws Exception {
        when(folderService.getFolerById(3L)).thenThrow(new FolderNotFoundException("Folder missing"));
        mvc.perform(get("/folders/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Folder missing"));
    }

    @Test
    void getFolders_usesEmptyNameWhenQueryIsOmitted() throws Exception {
        when(folderService.getFoldersBy(7L, "")).thenReturn(List.of(folder(1L, "Biology")));
        mvc.perform(get("/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Biology"));
        verify(folderService).getFoldersBy(7L, "");
    }

    @Test
    void getFolders_passesNameQuery() throws Exception {
        when(folderService.getFoldersBy(7L, "Bio")).thenReturn(List.of(folder(1L, "Biology")));
        mvc.perform(get("/folders").param("name", "Bio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        verify(folderService).getFoldersBy(7L, "Bio");
    }

    @Test
    void getFolders_returns404WhenServiceCannotFindFolders() throws Exception {
        when(folderService.getFoldersBy(7L, "")).thenThrow(new FolderNotFoundException("No folders"));
        mvc.perform(get("/folders")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No folders"));
    }

    @Test
    void getFavoriteFolders_usesEmptyNameWhenQueryIsOmitted() throws Exception {
        when(folderService.getFavoriteFoldersBy(7L, "")).thenReturn(List.of(folder(2L, "Chemistry")));
        mvc.perform(get("/folders/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Chemistry"));
        verify(folderService).getFavoriteFoldersBy(7L, "");
    }

    @Test
    void getFavoriteFolders_passesNameQuery() throws Exception {
        when(folderService.getFavoriteFoldersBy(7L, "Chem")).thenReturn(List.of(folder(2L, "Chemistry")));
        mvc.perform(get("/folders/favorite").param("name", "Chem"))
                .andExpect(status().isOk());
        verify(folderService).getFavoriteFoldersBy(7L, "Chem");
    }

    @Test
    void getFavoriteFolders_returns404WhenServiceThrows() throws Exception {
        when(folderService.getFavoriteFoldersBy(7L, "")).thenThrow(new FolderNotFoundException("No folders"));
        mvc.perform(get("/folders/favorite")).andExpect(status().isNotFound());
    }

    @Test
    void getFolderWithFlashcards_returnsFolderItem() throws Exception {
        var item = new FolderItemDto();
        item.setId(9L);
        item.setSummary("Chapter 1");
        item.setFlashcards(List.of(new FlashcardDto("c1", "Q", "A")));
        when(folderService.getFolerItems(9L)).thenReturn(item);

        mvc.perform(get("/folders/9/flashcards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.summary").value("Chapter 1"))
                .andExpect(jsonPath("$.flashcards[0].id").value("c1"));
    }

    @Test
    void getFolderWithFlashcards_returns404WhenMissing() throws Exception {
        when(folderService.getFolerItems(9L)).thenThrow(new FolderNotFoundException("Folder missing"));
        mvc.perform(get("/folders/9/flashcards")).andExpect(status().isNotFound());
    }

    @Test
    void createFolder_returns201AndLocation() throws Exception {
        when(folderService.isFolderNameUnique("Biology", 7L)).thenReturn(true);
        when(folderService.createFolder(any(CreateFolderRequest.class), eq(7L))).thenReturn(folder(12L, "Biology"));

        mvc.perform(post("/folders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Biology","fileUrl":"https://files.test/a.pdf","summary":"Intro"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/folders/12"))
                .andExpect(jsonPath("$.id").value(12));
        verify(folderService).createFolder(any(CreateFolderRequest.class), eq(7L));
    }

    @Test
    void createFolder_returns400WhenNoCurrentUser() throws Exception {
        when(authService.getCurrentUser()).thenReturn(null);
        mvc.perform(post("/folders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Biology\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User does not exist"));
        verify(folderService, never()).createFolder(any(), anyLong());
    }

    @Test
    void createFolder_returns400WhenNameIsNotUnique() throws Exception {
        when(folderService.isFolderNameUnique("Biology", 7L)).thenReturn(false);
        mvc.perform(post("/folders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Biology\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Folder name must be unique for each user"));
        verify(folderService, never()).createFolder(any(), anyLong());
    }

    @Test
    void createFolder_returns404WhenUniquenessCheckCannotFindUserFolders() throws Exception {
        when(folderService.isFolderNameUnique("Biology", 7L))
                .thenThrow(new FolderNotFoundException("No folders for user"));
        mvc.perform(post("/folders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Biology\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addFlashcards_returns201() throws Exception {
        when(folderService.addToFolder(eq(5L), anyList()))
                .thenReturn(List.of(new FlashcardDto("f1", "Question", "Answer")));
        mvc.perform(post("/folders/5/flashcards").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"question\":\"Question\",\"answer\":\"Answer\"}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value("f1"));
    }

    @Test
    void addFlashcards_returns404WhenFolderMissing() throws Exception {
        when(folderService.addToFolder(eq(5L), anyList()))
                .thenThrow(new FolderNotFoundException("Folder missing"));
        mvc.perform(post("/folders/5/flashcards").contentType(MediaType.APPLICATION_JSON)
                .content("[{\"question\":\"Q\",\"answer\":\"A\"}]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateFlashcards_returns201() throws Exception {
        when(folderService.updateFlashcardsInFolder(eq(5L), any(), any()))
                .thenReturn(List.of(new FlashcardDto("f1", "Q", "A")));
        mvc.perform(put("/folders/5/flashcards").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"existingCards":[{"id":"f1","question":"Q","answer":"A"}],
                         "removedCards":[{"id":"f2","question":"old","answer":"old"}]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value("f1"));
    }

    @Test
    void updateFlashcards_returns404WhenFolderMissing() throws Exception {
        when(folderService.updateFlashcardsInFolder(eq(5L), any(), any()))
                .thenThrow(new FolderNotFoundException("Folder missing"));
        mvc.perform(put("/folders/5/flashcards").contentType(MediaType.APPLICATION_JSON)
                .content("{\"existingCards\":[],\"removedCards\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFolder_returns204() throws Exception {
        mvc.perform(delete("/folders/5")).andExpect(status().isNoContent());
        verify(folderService).deleteFolder(5L);
    }

    @Test
    void deleteFolder_returns404WhenMissing() throws Exception {
        doThrow(new FolderNotFoundException("Folder missing")).when(folderService).deleteFolder(5L);
        mvc.perform(delete("/folders/5")).andExpect(status().isNotFound());
    }

    @Test
    void updateFavoriteStatus_supportsTrue() throws Exception {
        mvc.perform(put("/folders/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isFavorite\":true}"))
                .andExpect(status().isNoContent());
        verify(folderService).updateFavoriteStatus(5L, true);
    }

    @Test
    void updateFavoriteStatus_supportsFalse() throws Exception {
        mvc.perform(put("/folders/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isFavorite\":false}"))
                .andExpect(status().isNoContent());
        verify(folderService).updateFavoriteStatus(5L, false);
    }

    @Test
    void updateFavoriteStatus_returns404WhenMissing() throws Exception {
        doThrow(new FolderNotFoundException("Folder missing"))
                .when(folderService).updateFavoriteStatus(5L, true);
        mvc.perform(put("/folders/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isFavorite\":true}"))
                .andExpect(status().isNotFound());
    }
}
