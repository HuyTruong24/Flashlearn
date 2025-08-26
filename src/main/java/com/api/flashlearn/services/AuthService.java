package com.api.flashlearn.services;

import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.PasswordResetTokenDto;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.exceptions.TokenNotFoundException;
import com.api.flashlearn.mappers.PasswordResetTokenMapper;
import com.api.flashlearn.repositories.PasswordResetTokenRepository;
import com.api.flashlearn.repositories.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenMapper passwordResetTokenMapper;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) authentication.getPrincipal();
        return userRepository.findById(userId).orElse(null);
    }

    public PasswordResetTokenDto createPasswordResetToken(String email) {
        var user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid email"));
        String token = UUID.randomUUID().toString();

        var passwordResetToken = passwordResetTokenMapper.toEntity(token, user.getId());
        passwordResetTokenRepository.save(passwordResetToken);

        return passwordResetTokenMapper.toDto(passwordResetToken);
    }

    public void validatePasswordResetToken(Long tokenId, String token) {
        var passwordResetToken = passwordResetTokenRepository.findById(tokenId)
                .orElseThrow(() -> new TokenNotFoundException());

        if (!passwordResetToken.getToken().equals(token)) {
            throw new BadCredentialsException("Invalid token");
        }

        if (passwordResetToken.isExpired()) {
            throw new BadCredentialsException("Token has expired");
        }
    }
}
