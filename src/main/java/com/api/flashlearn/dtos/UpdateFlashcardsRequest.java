package com.api.flashlearn.dtos;

import java.util.List;

import lombok.Data;

@Data
public class UpdateFlashcardsRequest {
    private List<FlashcardDto> existingCards;
    private List<FlashcardDto> removedCards;
}
