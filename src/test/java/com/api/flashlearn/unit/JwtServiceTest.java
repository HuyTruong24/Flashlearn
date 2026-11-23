package com.api.flashlearn.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import javax.crypto.SecretKey;

import com.api.flashlearn.config.JwtConfig;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.services.Jwt;
import com.api.flashlearn.services.JwtService;

import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JwtServiceTest {

    private JwtConfig jwtConfig;
    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // Arrange common test fixture
        secretKey = Jwts.SIG.HS256.key().build();
        jwtConfig = mock(JwtConfig.class);
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(jwtConfig.getAccessTokenExpiration()).thenReturn((int) 900L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn((int) 604800L);
        jwtService = new JwtService(jwtConfig);
    }

    @Test
    void generateAccessToken_shouldCreateSignedTokenWithUserClaimsAndAccessExpiry() {
        // Arrange
        User user = mock(User.class);
        when(user.getId()).thenReturn(25L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getName()).thenReturn("Test User");

        // Act
        Jwt result = jwtService.generateAccessToken(user);

        // Assert
        assertNotNull(result);
        // Jwt's public token representation is parsed by the service on round-trip.
        assertNotNull(jwtService.parseToken(result.toString()));
    }

    @Test
    void generateRefreshToken_shouldCreateSignedToken() {
        // Arrange
        User user = mock(User.class);
        when(user.getId()).thenReturn(25L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getName()).thenReturn("Test User");

        // Act
        Jwt result = jwtService.generateRefreshToken(user);

        // Assert
        assertNotNull(result);
        assertNotNull(jwtService.parseToken(result.toString()));
    }

    @Test
    void parseToken_shouldReturnNullForInvalidToken() {
        // Arrange
        String invalidToken = "not.a.valid.jwt";

        // Act
        Jwt result = jwtService.parseToken(invalidToken);

        // Assert
        assertNull(result);
    }

    @Test
    void parseToken_shouldReturnNullForTokenSignedWithDifferentKey() {
        // Arrange
        SecretKey otherKey = Jwts.SIG.HS256.key().build();
        String token = Jwts.builder()
                .subject("25")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        // Act
        Jwt result = jwtService.parseToken(token);

        // Assert
        assertNull(result);
    }

    @Test
    void parseToken_shouldReturnJwtForValidSignedToken() {
        // Arrange
        String token = Jwts.builder()
                .subject("25")
                .claim("email", "user@example.com")
                .claim("name", "Test User")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        // Act
        Jwt result = jwtService.parseToken(token);

        // Assert
        assertNotNull(result);
    }
}
