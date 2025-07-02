package com.api.flashlearn.controllers;


import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.flashlearn.dtos.RegisterUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping("/hello")
    public String hello() {
        return new String("yall see this");
    }

    @GetMapping
    public Iterable<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable(name = "username") String username) {
        var user = userService.getUserByUsername(username);
        if(user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterUserRequest request, UriComponentsBuilder uriBuilder) {
        if(!request.getUsername().equals(request.getRetypeUsername())) {
            return ResponseEntity.badRequest().body(Map.of("username","Usernames do not match"));
        }
        if(userService.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("username","Username already exists"));
        }

        var userDto = userService.createUser(request);
        var uri = uriBuilder.path("/users/{username}").buildAndExpand(userDto.getUsername()).toUri();
        
        return ResponseEntity.created(uri).body(userDto);
    }
    
    
    
}
