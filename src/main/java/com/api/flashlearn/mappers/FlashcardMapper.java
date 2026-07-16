package com.api.flashlearn.mappers;

import org.mapstruct.Mapper;

import com.api.flashlearn.dtos.CreateFlashcardRequest;
import com.api.flashlearn.dtos.FlashcardDto;
import com.api.flashlearn.entities.Flashcard;

@Mapper(componentModel = "spring")
public interface FlashcardMapper {
    FlashcardDto toDto(Flashcard flashcard);

    Flashcard toEntity(CreateFlashcardRequest flashcardDto);

    Flashcard toEntity(FlashcardDto flashcardDto);
}
