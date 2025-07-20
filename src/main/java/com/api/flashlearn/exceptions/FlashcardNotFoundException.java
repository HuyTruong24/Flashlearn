package com.api.flashlearn.exceptions;

public class FlashcardNotFoundException extends RuntimeException{
    public FlashcardNotFoundException(String message) {
        super(message);
    }
}
