package com.api.flashlearn.dtos;

import lombok.Data;

@Data
public class CreateFlashcardRequest {
    private String question;
    private String answer;
}
