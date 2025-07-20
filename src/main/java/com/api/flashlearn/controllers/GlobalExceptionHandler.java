package com.api.flashlearn.controllers;


import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@ControllerAdvice
public class GlobalExceptionHandler {
     /**
     * Handles MethodArgumentNotValidException for attributes of dtos.
     *
     * @param exception the MethodArgumentNotValidException
     * @return a ResponseEntity containing the validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }
    /**
     * Handles WebClientResponseException for validation errors.
     *
     * @param exception the WebClientResponseException
     * @return a ResponseEntity containing error message
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(WebClientResponseException exception) {
        System.out.println("WebClientResponseException: " + exception.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error",exception.getMessage()));
    }
}
