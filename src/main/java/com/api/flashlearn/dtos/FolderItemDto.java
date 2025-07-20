package com.api.flashlearn.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class FolderItemDto {
    private Long id;
    private List<FlashcardDto> flashcards = new ArrayList<>();
    private String summary;
}
