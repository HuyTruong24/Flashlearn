package com.api.flashlearn.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.RegisterUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.entities.User;
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
}
