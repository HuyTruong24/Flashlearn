package com.api.flashlearn.services;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.api.flashlearn.config.JwtConfig;
import com.api.flashlearn.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;

@Service 
@AllArgsConstructor
public class JwtService {
    private final JwtConfig jwtConfig;
    public Jwt generateAccessToken(User user) {
        return generateToken(user, jwtConfig.getAccessTokenExpiration());
    }
    public Jwt generateRefreshToken(User user) {
        return generateToken(user, jwtConfig.getRefreshTokenExpiration());
    }
    private Jwt generateToken(User user, long expirationTime){
        var claims = Jwts.claims()
            .subject(user.getId().toString())
            .add("email", user.getEmail())
            .add("name", user.getName())
            .issuedAt(new Date()) 
            .expiration(new Date(System.currentTimeMillis() + 1000 * expirationTime))
            .build();

        return new Jwt(claims, jwtConfig.getSecretKey());

        
    }
    public Jwt parseToken(String token) {
        try {
            var claims = getClaims(token);
            return new Jwt(claims, jwtConfig.getSecretKey());
        } catch (JwtException e) {
            return null;
        }
    }
    private Claims getClaims(String token){
        return Jwts.parser().verifyWith(jwtConfig.getSecretKey())
            .build().parseSignedClaims(token).getPayload();
    }

}
