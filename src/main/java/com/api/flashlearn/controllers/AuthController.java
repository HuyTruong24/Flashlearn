package com.api.flashlearn.controllers;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.flashlearn.config.JwtConfig;
import com.api.flashlearn.dtos.ResetPasswordRequest;
import com.api.flashlearn.dtos.VerifyUserRequest;
import com.api.flashlearn.dtos.EmailVerificationRequest;
import com.api.flashlearn.dtos.ErrorDto;
import com.api.flashlearn.dtos.JwtResponse;
import com.api.flashlearn.dtos.LoginRequest;
import com.api.flashlearn.dtos.PasswordResetTokenDto;
import com.api.flashlearn.dtos.RegisterUserRequest;

import com.api.flashlearn.exceptions.AccountNotVerifiedException;
import com.api.flashlearn.exceptions.EmailInUseException;
import com.api.flashlearn.exceptions.PasswordMismatchException;
import com.api.flashlearn.exceptions.TokenNotFoundException;

import com.api.flashlearn.repositories.UserRepository;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.JwtService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.Map;

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
    
    /**
     * Endpoint to authenticate a user and generate JWT tokens.
     * @param request
     * @param response
     * @return ResponseEntity with JwtResponse containing the access token
     * @throws BadCredentialsException if the email or password is incorrect
     * @throws AccountNotVerifiedException if the user's account is not verified
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        if(!user.isEnabled()) {
            throw new AccountNotVerifiedException();
        }

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

    /**
     * Endpoint to register a new user.
     * @param request
     * @param uriBuilder
     * @return ResponseEntity with UserDto and location header
     * @throws EmailInUseException if the email is already in use and activated by another verified user
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterUserRequest request, UriComponentsBuilder uriBuilder) {
        var userDto = authService.register(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(userDto.getId()).toUri();
        
        return ResponseEntity.created(uri).body(userDto);
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
    /**
     * Endpoint to verify email and create a password reset token.
     * @param request
     * @return ResponseEntity with PasswordResetTokenDto
     * @throws BadCredentialsException if the email does not exist in the system
     */
    @PostMapping("/verify-email-for-password-reset")
    public ResponseEntity<PasswordResetTokenDto> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        var tokenDto = authService.createPasswordResetToken(request.getEmail());
        return ResponseEntity.ok().body(tokenDto);
    }
    
    /**
     * Endpoint to reset a user's password using a valid token.
     * @param userId 
     * @param tokenId
     * @param request
     * @return ResponseEntity with no content
     * @throws PasswordMismatchException if the new password and confirm password do not match
     * @throws TokenNotFoundException if the token is invalid or does not exist
     */
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

    /**
     * Endpoint to verify a user's account using a verification code.
     * @param request
     * @return ResponseEntity with success message
     * @throws BadCredentialsException if the email is invalid, user is already verified,
     *         verification code is invalid, or verification code has expired
     */
    @PostMapping("/verify-user")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserRequest request) {
        authService.verifyUser(request);
        return ResponseEntity.ok().body(Map.of("message", "Account verified successfully"));
    }

    /**
     * Endpoint to resend the verification code to a user's email.
     * @param request
     * @return ResponseEntity with success message
     * @throws BadCredentialsException if the email is invalid or user is already verified
     */
    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestBody EmailVerificationRequest request) {
        authService.resendVerificationCode(request.getEmail());
        return ResponseEntity.ok().body(Map.of("message", "Verification code sent"));
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

    /**
     * Exception handler for EmailInUseException.
     * @return ResponseEntity with error message and 400 Bad Request status
     */
    @ExceptionHandler(EmailInUseException.class)
    public ResponseEntity<ErrorDto> handleEmailInUseException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("Email is already in use"));
    }
    /**
     * Exception handler for PasswordMismatchException.
     * @return ResponseEntity with error message and 400 Bad Request status
     */
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorDto> handlePasswordMismatchException(PasswordMismatchException exception) {
        return ResponseEntity.badRequest().body(new ErrorDto(exception.getMessage()));
    }
    /**
     * Exception handler for BadCredentialsException.
     * @return ResponseEntity with 401 Unauthorized status
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    /**
     * Exception handler for AccountNotVerifiedException.
     * @param ex
     * @return ResponseEntity with error message and 403 Forbidden status
     */
    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<ErrorDto> handleAccountNotVerifiedException(AccountNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorDto(ex.getMessage()));
    }
    /**
     * Exception handler for TokenNotFoundException.
     * @return ResponseEntity with 404 Not Found status
     */
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<Void> handleTokenNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    
}
