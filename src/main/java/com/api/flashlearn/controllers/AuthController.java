package com.api.flashlearn.controllers;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.flashlearn.config.JwtConfig;
import com.api.flashlearn.dtos.ResetPasswordRequest;
import com.api.flashlearn.dtos.EmailVerificationRequest;
import com.api.flashlearn.dtos.JwtResponse;
import com.api.flashlearn.dtos.LoginRequest;
import com.api.flashlearn.dtos.PasswordResetTokenDto;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.exceptions.PasswordMismatchException;
import com.api.flashlearn.exceptions.TokenNotFoundException;
import com.api.flashlearn.exceptions.UserNotFoundException;
import com.api.flashlearn.repositories.UserRepository;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.JwtService;
import com.api.flashlearn.services.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        var cookie = new Cookie("refreshToken", refreshToken.toString());
        cookie.setHttpOnly(true); //cannot be accessed by JavaScript
        cookie.setPath("/auth/refresh"); 
        cookie.setMaxAge(jwtConfig.getRefreshTokenExpiration()); // cookie expires in 7 days (should match the refresh token expiration)
        cookie.setSecure(true); // only sent over HTTPS
        response.addCookie(cookie); // add the cookie to the response
        
        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }
    /*
     * This endpoint is used to refresh the access token using the refresh token stored in a cookie.
     * It checks if the refresh token is valid and not expired, then generates a new access token.
     * If the refresh token is invalid or expired, it returns a 401 Unauthorized response.
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@CookieValue(value = "refreshToken") String refreshToken) {
        var jwt = jwtService.parseToken(refreshToken);
       if(jwt == null || jwt.isExpired()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userId = jwt.getUserId();
        var user = userRepository.findById(userId).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user);
        
        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }
    @PostMapping("/verify-email-for-password-reset")
    public ResponseEntity<PasswordResetTokenDto> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        var tokenDto = authService.createPasswordResetToken(request.getEmail());
        return ResponseEntity.ok().body(tokenDto);
    }
    
    @PostMapping("/reset-password/{userId}/{tokenId}")
    public ResponseEntity<Void> resetPassword(
        @PathVariable Long userId,
        @PathVariable Long tokenId,
        @Valid @RequestBody ResetPasswordRequest request) {

        if(!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        authService.validatePasswordResetToken(tokenId, request.getToken());

        var user = userRepository.findById(userId).orElseThrow();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        var cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0); // expire the cookie immediately
        cookie.setSecure(true); // only sent over HTTPS
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<Void> handleTokenNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    
}
