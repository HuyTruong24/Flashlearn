package com.api.flashlearn.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.flashlearn.dtos.PasswordResetTokenDto;
import com.api.flashlearn.dtos.RegisterUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.dtos.VerifyUserRequest;
import com.api.flashlearn.entities.User;
import com.api.flashlearn.exceptions.EmailInUseException;
import com.api.flashlearn.exceptions.TokenNotFoundException;
import com.api.flashlearn.mappers.PasswordResetTokenMapper;
import com.api.flashlearn.mappers.UserMapper;
import com.api.flashlearn.repositories.PasswordResetTokenRepository;
import com.api.flashlearn.repositories.UserRepository;

import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {
    private static final int VERIFICATION_CODE_DURATION_MINUTES = 5;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) authentication.getPrincipal();
        return userRepository.findById(userId).orElse(null);
    }

    public UserDto register(RegisterUserRequest request) {
        var registeredUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (registeredUser != null) {
            if(registeredUser.isEnabled()) {
                throw new EmailInUseException();
            }
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setCodeExpiryTime(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_DURATION_MINUTES));
        user.setEnabled(false);
        sendVerificationEmail(user);
        userRepository.save(user);

        return userMapper.toDto(user);
    }
    
    public void verifyUser(VerifyUserRequest input) {
        var user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        if (user.isEnabled()) {
            throw new BadCredentialsException("User is already verified");
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new BadCredentialsException("Invalid verification code");
        }

        if (user.getCodeExpiryTime() == null || user.getCodeExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Verification code has expired");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setCodeExpiryTime(null);
        userRepository.save(user);
    }

    public void resendVerificationCode(String email) {
         var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        if (user.isEnabled()) {
            throw new BadCredentialsException("User is already verified");
        }

        user.setVerificationCode(generateVerificationCode());
        user.setCodeExpiryTime(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_DURATION_MINUTES));
        sendVerificationEmail(user);
        userRepository.save(user);
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

    private void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to Flashlearn!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    private String generateVerificationCode() {
        int code = (int)(Math.random() * 900000) + 100000; // Generate a random 6-digit code
        return String.valueOf(code);
    }
}
