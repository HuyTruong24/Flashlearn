package com.api.flashlearn.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.RegisterUserRequest;
import com.api.flashlearn.dtos.UpdateUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.exceptions.UserNotFoundException;
import com.api.flashlearn.mappers.UserMapper;
import com.api.flashlearn.repositories.UserRepository;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUserByUsername(String username) {
        return userRepository.findById(username)
                .map(userMapper::toDto)
                .orElse(null);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public UserDto createUser(RegisterUserRequest request) {
        User user = userMapper.toEntity(request);
        userRepository.save(user);
        return userMapper.toDto(user);
    }
    public UserDto updateUser(UpdateUserRequest request) {
        User user = userRepository.findById(request.getUsername()).orElseThrow(() -> new UserNotFoundException());
        
        userMapper.updateUserRequest(request, user);
        userRepository.save(user);
        
        return userMapper.toDto(user);
    }

    public void deleteUser(String username) {
        User user = userRepository.findById(username).orElseThrow(() -> new UserNotFoundException());
        userRepository.delete(user);
    }
}
