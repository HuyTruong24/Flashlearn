package com.api.flashlearn.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.flashlearn.dtos.UpdateUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.exceptions.UserNotFoundException;
import com.api.flashlearn.mappers.UserMapper;
import com.api.flashlearn.repositories.UserRepository;
import com.api.flashlearn.services.UserService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        userDto = mock(UserDto.class);
    }

    // =========================
    // getAllUsers()
    // =========================

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        // Arrange
        User user2 = mock(User.class);
        UserDto userDto2 = mock(UserDto.class);

        when(userRepository.findAll())
                .thenReturn(List.of(user, user2));

        when(userMapper.toDto(user)).thenReturn(userDto);
        when(userMapper.toDto(user2)).thenReturn(userDto2);

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertEquals(2, result.size());
        assertEquals(userDto, result.get(0));
        assertEquals(userDto2, result.get(1));

        verify(userRepository).findAll();
        verify(userMapper).toDto(user);
        verify(userMapper).toDto(user2);
    }

    @Test
    void getAllUsers_shouldReturnEmptyListWhenNoUsersExist() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findAll();
        verifyNoInteractions(userMapper);
    }

    // =========================
    // getById()
    // =========================

    @Test
    void getById_shouldReturnUserWhenFound() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userMapper.toDto(user)).thenReturn(userDto);

        // Act
        UserDto result = userService.getById(userId);

        // Assert
        assertEquals(userDto, result);

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    void getById_shouldReturnNullWhenUserNotFound() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act
        UserDto result = userService.getById(userId);

        // Assert
        assertNull(result);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }

    // =========================
    // getByEmail()
    // =========================

    @Test
    void getByEmail_shouldReturnUserWhenFound() {
        // Arrange
        String email = "test@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(userMapper.toDto(user)).thenReturn(userDto);

        // Act
        UserDto result = userService.getByEmail(email);

        // Assert
        assertEquals(userDto, result);

        verify(userRepository).findByEmail(email);
        verify(userMapper).toDto(user);
    }

    @Test
    void getByEmail_shouldReturnNullWhenUserNotFound() {
        // Arrange
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        // Act
        UserDto result = userService.getByEmail(email);

        // Assert
        assertNull(result);

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(userMapper);
    }

    // =========================
    // existsByEmail()
    // =========================

    @Test
    void existsByEmail_shouldReturnTrueWhenEmailExists() {
        // Arrange
        String email = "test@example.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertTrue(result);

        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_shouldReturnFalseWhenEmailDoesNotExist() {
        // Arrange
        String email = "unknown@example.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertFalse(result);

        verify(userRepository).existsByEmail(email);
    }

    // =========================
    // save()
    // =========================

    @Test
    void save_shouldSaveUser() {
        // Arrange
        // User is initialized in setUp()

        // Act
        userService.save(user);

        // Assert
        verify(userRepository).save(user);
    }

    // =========================
    // updateUser()
    // =========================

    @Test
    void updateUser_shouldUpdateAndReturnUser() {
        // Arrange
        Long userId = 1L;
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userMapper.toDto(user)).thenReturn(userDto);

        // Act
        UserDto result = userService.updateUser(userId, request);

        // Assert
        assertEquals(userDto, result);

        verify(userRepository).findById(userId);
        verify(userMapper).updateUserRequest(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @Test
    void updateUser_shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Long userId = 1L;
        UpdateUserRequest request = mock(UpdateUserRequest.class);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(userId, request));

        // Additional interaction assertions
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper);
    }

    // =========================
    // deleteUser()
    // =========================

    @Test
    void deleteUser_shouldDeleteUserWhenFound() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(userId));

        // Additional interaction assertions
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
    }
}
