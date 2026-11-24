package com.api.flashlearn.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.api.flashlearn.controllers.UserController;
import com.api.flashlearn.dtos.UpdateUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.JwtService;
import com.api.flashlearn.services.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = mock(User.class);
        when(currentUser.getId()).thenReturn(42L);
        when(authService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void getAllUsers_returnsUsers() throws Exception {
        var user = new UserDto(1L, "a@example.com", "Alice", null, null);
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("a@example.com"))
                .andExpect(jsonPath("$[0].name").value("Alice"));
        verify(userService).getAllUsers();
    }

    @Test
    void getAllUsers_returnsEmptyArrayWhenNoUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());
        mvc.perform(get("/users")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void getCurrentUser_returnsUser() throws Exception {
        when(userService.getById(42L))
                .thenReturn(new UserDto(42L, "me@example.com", "Me", null, null));

        mvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("me@example.com"));
        verify(userService).getById(42L);
    }

    @Test
    void getCurrentUser_returns404WhenServiceReturnsNull() throws Exception {
        when(userService.getById(42L)).thenReturn(null);
        mvc.perform(get("/users/me")).andExpect(status().isNotFound());
    }

    @Test
    void getCurrentUser_returns404WhenUserNotFoundExceptionIsThrown() throws Exception {
        when(userService.getById(42L)).thenThrow(new com.api.flashlearn.exceptions.UserNotFoundException());
        mvc.perform(get("/users/me")).andExpect(status().isNotFound());
    }

    @Test
    void updateCurrentUser_returnsUpdatedUser() throws Exception {
        when(userService.updateUser(eq(42L), any(UpdateUserRequest.class)))
                .thenReturn(new UserDto(42L, "me@example.com", "Updated", "https://img.test/me.png", null));

        mvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Updated","profileImgUrl":"https://img.test/me.png"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.profileImgUrl").value("https://img.test/me.png"));
        verify(userService).updateUser(eq(42L), any(UpdateUserRequest.class));
    }

    @Test
    void deleteCurrentUser_returns204() throws Exception {
        mvc.perform(delete("/users/me")).andExpect(status().isNoContent());
        verify(userService).deleteUser(42L);
    }

    @Test
    void deleteCurrentUser_mapsUserNotFoundTo404() throws Exception {
        doThrow(new com.api.flashlearn.exceptions.UserNotFoundException())
                .when(userService).deleteUser(42L);
        mvc.perform(delete("/users/me")).andExpect(status().isNotFound());
    }
}
