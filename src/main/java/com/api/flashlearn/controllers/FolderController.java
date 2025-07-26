package com.api.flashlearn.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.CreateFolderRequest;
import com.api.flashlearn.dtos.ErrorDto;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.dtos.FolderDto;
import com.api.flashlearn.dtos.FolderItemDto;
import com.api.flashlearn.exceptions.DuplicateFolderNameException;
import com.api.flashlearn.exceptions.FolderNotFoundException;
import com.api.flashlearn.mappers.FolderMapper;
import com.api.flashlearn.repositories.FolderRespository;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.FolderService;
import com.api.flashlearn.services.UserService;

import lombok.AllArgsConstructor;


import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/folders")
@AllArgsConstructor
public class FolderController {
    private final FolderService folderService;
    private final AuthService authService;

    @GetMapping("/{folderId}")
    public FolderDto getFolderById(@PathVariable Long folderId) {
        return folderService.getFolerById(folderId);
    }

    @GetMapping
    public List<FolderDto> getFoldersOfCurrentUser() {
        var user = authService.getCurrentUser();
        var folderDtos = folderService.getFoldersBy(user.getId());
        return folderDtos;
    }
    
    @GetMapping("/{folderId}/flashcards")
    public ResponseEntity<FolderItemDto> getFolderWithFlashcards(@PathVariable Long folderId) {
        var folderItemDto = folderService.getFolerItems(folderId);
        return ResponseEntity.ok(folderItemDto);
    }
    
    @PostMapping
    public ResponseEntity<?> createFolder(@RequestBody CreateFolderRequest request, UriComponentsBuilder uriBuilder) {
        var user = authService.getCurrentUser();
        if(user == null){
            return ResponseEntity.badRequest().body(Map.of("error","User does not exist"));
        }

        if(!folderService.isFolderNameUnique(request.getName(), user.getId())) {
            throw new DuplicateFolderNameException("Folder name must be unique for each user");
        }


        var folderDto = folderService.createFolder(request, user.getId());

        var uri = uriBuilder.path("/folders/{id}").buildAndExpand(folderDto.getId()).toUri();
        
        return ResponseEntity.created(uri).body(folderDto);
    }

    @PostMapping("/{folderId}/flashcards")
    public ResponseEntity<List<FlashcardDto>> addCardsToFolder(
        @PathVariable Long folderId,
        @RequestBody List<CreateFlashcardRequest> request) {
        
        List<FlashcardDto> flashcardDtos = folderService.addToFolder(folderId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(flashcardDtos);
    }
    

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
        folderService.deleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<ErrorDto> handleFolderNotFound(FolderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateFolderNameException.class)
    public ResponseEntity<ErrorDto> handleDuplicateFolderName(DuplicateFolderNameException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ex.getMessage()));
    }

    
}
