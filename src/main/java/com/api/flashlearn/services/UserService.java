package com.api.flashlearn.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.UserDto;
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
}
