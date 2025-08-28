package com.api.flashlearn.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.entities.Flashcard;
import com.api.flashlearn.entities.Folder;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {
    FlashcardDto toDto(Flashcard flashcard);
    Flashcard toEntity(CreateFlashcardRequest flashcardDto);
    Flashcard toEntity(FlashcardDto flashcardDto);
}
