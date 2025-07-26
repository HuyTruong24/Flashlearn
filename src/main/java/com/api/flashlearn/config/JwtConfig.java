package com.api.flashlearn.config;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.security.Keys;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "spring.jwt") //bind spring.jwt in application.yaml to this class
// This class is used to hold JWT configuration properties such as secret key, expiration times, etc.
@Data
public class JwtConfig {
    private String secret; // JWT secret key
    private int accessTokenExpiration; // Access token expiration time in seconds
    private int refreshTokenExpiration; // Refresh token expiration time in seconds

    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes()); // Convert the secret key string to a SecretKey object
    }
    
}
