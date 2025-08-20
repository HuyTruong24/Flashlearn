package com.api.flashlearn.controllers;


import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.flashlearn.dtos.RegisterUserRequest;
import com.api.flashlearn.dtos.UpdateUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.exceptions.UserNotFoundException;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public Iterable<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getUser() {
        var currentUser = authService.getCurrentUser();
        var user = userService.getById(currentUser.getId());
        if(user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterUserRequest request, UriComponentsBuilder uriBuilder) {
        if(userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("email","Email already exists"));
        }

        var userDto = userService.createUser(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getId()).toUri();
        
        return ResponseEntity.created(uri).body(userDto);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateUserInfo(@RequestBody UpdateUserRequest request) {
        var currentUser = authService.getCurrentUser();
        var userDto = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser() {
        var currentUser = authService.getCurrentUser();
        userService.deleteUser(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.notFound().build();   
    }
    
    
    
}
