package com.api.flashlearn.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import com.api.flashlearn.dtos.*;
import com.api.flashlearn.entities.*;
import com.api.flashlearn.exceptions.*;
import com.api.flashlearn.mappers.*;
import com.api.flashlearn.repositories.*;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.EmailService;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordResetTokenRepository passwordResetTokenRepository;
        @Mock
        private PasswordResetTokenMapper passwordResetTokenMapper;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private UserMapper userMapper;
        @Mock
        private EmailService emailService;

        @InjectMocks
        private AuthService authService;

        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        // getCurrentUser

        @Test
        void getCurrentUser_shouldReturnUserWhenFound() {
                // Arrange
                Long userId = 12L;
                Authentication authentication = mock(Authentication.class);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                when(authentication.getPrincipal()).thenReturn(userId);
                User user = mock(User.class);
                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                // Act
                User result = authService.getCurrentUser();

                // Assert
                assertSame(user, result);
                verify(userRepository).findById(userId);
        }

        @Test
        void getCurrentUser_shouldReturnNullWhenUserDoesNotExist() {
                // Arrange
                Long userId = 12L;
                Authentication authentication = mock(Authentication.class);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                when(authentication.getPrincipal()).thenReturn(userId);
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                // Act
                User result = authService.getCurrentUser();

                // Assert
                assertNull(result);
                verify(userRepository).findById(userId);
        }

        // register

        @Test
        void register_shouldCreateNewDisabledUserAndSendVerificationEmail() throws Exception {
                // Arrange
                RegisterUserRequest request = new RegisterUserRequest(
                                "new@example.com",
                                null,
                                "plain-password");

                User user = new User();
                user.setEmail(request.getEmail());
                UserDto expected = mock(UserDto.class);

                when(userRepository.findByEmail("new@example.com"))
                                .thenReturn(Optional.empty());

                when(userMapper.toEntity(request))
                                .thenReturn(user);

                when(passwordEncoder.encode("plain-password"))
                                .thenReturn("encoded-password");

                when(userMapper.toDto(user))
                                .thenReturn(expected);

                // Act
                UserDto result = authService.register(request);

                // Assert
                assertSame(expected, result);

                // Verify password encoding
                assertEquals("encoded-password", user.getPassword());

                // Verify generated verification code
                String verificationCode = user.getVerificationCode();

                assertNotNull(verificationCode);
                assertTrue(verificationCode.matches("\\d{6}"));

                // Verify account status
                assertFalse(user.isEnabled());

                // Verify verification code expiration
                assertNotNull(user.getCodeExpiryTime());
                assertTrue(user.getCodeExpiryTime().isAfter(LocalDateTime.now()));
                assertTrue(user.getCodeExpiryTime().isBefore(
                                LocalDateTime.now().plusMinutes(6)));

                // Verify verification email
                verify(emailService).sendEmail(
                                eq("new@example.com"),
                                eq("Account Verification"),
                                contains("VERIFICATION CODE " + verificationCode));

                // Verify persistence
                verify(userRepository).save(user);

                // Verify mapping
                verify(userMapper).toEntity(request);
                verify(userMapper).toDto(user);
        }

        @Test
        void register_shouldThrowWhenEmailBelongsToEnabledUser() {
                // Arrange
                RegisterUserRequest request = mock(RegisterUserRequest.class);
                User existing = mock(User.class);
                when(request.getEmail()).thenReturn("used@example.com");
                when(userRepository.findByEmail("used@example.com")).thenReturn(Optional.of(existing));
                when(existing.isEnabled()).thenReturn(true);

                // Act & Assert
                assertThrows(EmailInUseException.class, () -> authService.register(request));

                // Assert interactions
                verify(userRepository, never()).save(any());
                verifyNoInteractions(passwordEncoder, emailService, userMapper);
        }

        @Test
        void register_shouldReuseDisabledUserAndRefreshVerificationDetails() throws Exception {
                // Arrange
                RegisterUserRequest request = mock(RegisterUserRequest.class);
                User existing = mock(User.class);
                UserDto expected = mock(UserDto.class);
                when(request.getEmail()).thenReturn("pending@example.com");
                when(request.getPassword()).thenReturn("password");
                when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(existing));
                when(existing.isEnabled()).thenReturn(false);
                when(passwordEncoder.encode("password")).thenReturn("encoded");
                when(existing.getEmail()).thenReturn("pending@example.com");
                when(userMapper.toDto(existing)).thenReturn(expected);

                // Act
                UserDto result = authService.register(request);

                // Assert
                assertSame(expected, result);
                verify(userMapper, never()).toEntity(any());
                verify(existing).setPassword("encoded");
                verify(existing).setEnabled(false);
                verify(existing).setVerificationCode(argThat(code -> code != null && code.matches("\\d{6}")));
                verify(existing).setCodeExpiryTime(any(LocalDateTime.class));
                verify(emailService).sendEmail(eq("pending@example.com"), eq("Account Verification"),
                                contains("VERIFICATION CODE"));
                verify(userRepository).save(existing);
        }

        @Test
        void register_shouldContinueSavingWhenVerificationEmailThrowsMessagingException() throws Exception {
                // Arrange
                RegisterUserRequest request = mock(RegisterUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("new@example.com");
                when(request.getPassword()).thenReturn("password");
                when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
                when(userMapper.toEntity(request)).thenReturn(user);
                when(passwordEncoder.encode("password")).thenReturn("encoded");
                when(user.getEmail()).thenReturn("new@example.com");
                doThrow(new MessagingException("mail failure"))
                                .when(emailService)
                                .sendEmail(eq("new@example.com"), eq("Account Verification"), anyString());

                // Act
                assertDoesNotThrow(() -> authService.register(request));

                // Assert
                verify(userRepository).save(user);
        }

        // verifyUser

        @Test
        void verifyUser_shouldEnableUserAndClearVerificationFields() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("user@example.com");
                when(request.getVerificationCode()).thenReturn("123456");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(false);
                when(user.getVerificationCode()).thenReturn("123456");
                when(user.getCodeExpiryTime()).thenReturn(LocalDateTime.now().plusMinutes(2));

                // Act
                authService.verifyUser(request);

                // Assert
                verify(user).setEnabled(true);
                verify(user).setVerificationCode(null);
                verify(user).setCodeExpiryTime(null);
                verify(userRepository).save(user);
        }

        @Test
        void verifyUser_shouldThrowWhenEmailIsUnknown() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                when(request.getEmail()).thenReturn("missing@example.com");
                when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

                // Act & Assert
                BadCredentialsException exception = assertThrows(
                                BadCredentialsException.class, () -> authService.verifyUser(request));
                assertEquals("Invalid email", exception.getMessage());
                verify(userRepository, never()).save(any());
        }

        @Test
        void verifyUser_shouldThrowWhenUserIsAlreadyEnabled() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("user@example.com");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(true);

                // Act & Assert
                assertEquals("User is already verified",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.verifyUser(request)).getMessage());
                verify(userRepository, never()).save(any());
        }

        @Test
        void verifyUser_shouldThrowWhenStoredCodeIsNull() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("user@example.com");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(false);
                when(user.getVerificationCode()).thenReturn(null);

                // Act & Assert
                assertEquals("Invalid verification code",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.verifyUser(request)).getMessage());
                verify(userRepository, never()).save(any());
        }

        @Test
        void verifyUser_shouldThrowWhenCodeDoesNotMatch() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("user@example.com");
                when(request.getVerificationCode()).thenReturn("wrong");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(false);
                when(user.getVerificationCode()).thenReturn("123456");

                // Act & Assert
                assertEquals("Invalid verification code",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.verifyUser(request)).getMessage());
                verify(userRepository, never()).save(any());
        }

        @Test
        void verifyUser_shouldThrowWhenExpiryIsNull() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("user@example.com");
                when(request.getVerificationCode()).thenReturn("123456");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(false);
                when(user.getVerificationCode()).thenReturn("123456");
                when(user.getCodeExpiryTime()).thenReturn(null);

                // Act & Assert
                assertEquals("Verification code has expired",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.verifyUser(request)).getMessage());
                verify(userRepository, never()).save(any());
        }

        @Test
        void verifyUser_shouldThrowWhenCodeHasExpired() {
                // Arrange
                VerifyUserRequest request = mock(VerifyUserRequest.class);
                User user = mock(User.class);
                when(request.getEmail()).thenReturn("user@example.com");
                when(request.getVerificationCode()).thenReturn("123456");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(false);
                when(user.getVerificationCode()).thenReturn("123456");
                when(user.getCodeExpiryTime()).thenReturn(LocalDateTime.now().minusSeconds(1));

                // Act & Assert
                assertEquals("Verification code has expired",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.verifyUser(request)).getMessage());
                verify(userRepository, never()).save(any());
        }

        // resendVerificationCode

        @Test
        void resendVerificationCode_shouldGenerateNewCodeSendEmailAndSave() throws Exception {
                // Arrange
                String email = "user@example.com";

                User user = new User();
                user.setEmail(email);
                user.setEnabled(false);

                when(userRepository.findByEmail(email))
                                .thenReturn(Optional.of(user));

                // Act
                authService.resendVerificationCode(email);

                // Assert
                String verificationCode = user.getVerificationCode();

                // Verify generated verification code
                assertNotNull(verificationCode);
                assertTrue(verificationCode.matches("\\d{6}"));

                // Verify expiration time is approximately 5 minutes from now
                assertNotNull(user.getCodeExpiryTime());
                assertTrue(user.getCodeExpiryTime().isAfter(LocalDateTime.now()));
                assertTrue(user.getCodeExpiryTime().isBefore(
                                LocalDateTime.now().plusMinutes(6)));

                // Verify email was sent with the generated code
                verify(emailService).sendEmail(
                                eq(email),
                                eq("Account Verification"),
                                contains("VERIFICATION CODE " + verificationCode));

                // Verify user was saved
                verify(userRepository).save(user);
        }

        @Test
        void resendVerificationCode_shouldThrowWhenEmailIsUnknown() {
                // Arrange
                String email = "missing@example.com";
                when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

                // Act & Assert
                assertEquals("Invalid email",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.resendVerificationCode(email)).getMessage());
                verify(userRepository, never()).save(any());
                verifyNoInteractions(emailService);
        }

        @Test
        void resendVerificationCode_shouldThrowWhenUserIsAlreadyEnabled() {
                // Arrange
                String email = "user@example.com";
                User user = mock(User.class);
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(user.isEnabled()).thenReturn(true);

                // Act & Assert
                assertEquals("User is already verified",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.resendVerificationCode(email)).getMessage());
                verify(userRepository, never()).save(any());
                verifyNoInteractions(emailService);
        }

        // createPasswordResetToken

        @Test
        void createPasswordResetToken_shouldCreateSaveAndReturnDto() {
                // Arrange
                String email = "user@example.com";
                User user = mock(User.class);
                PasswordResetToken tokenEntity = mock(PasswordResetToken.class);
                PasswordResetTokenDto expected = mock(PasswordResetTokenDto.class);
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(user.getId()).thenReturn(42L);
                when(passwordResetTokenMapper.toEntity(anyString(), eq(42L))).thenReturn(tokenEntity);
                when(passwordResetTokenMapper.toDto(tokenEntity)).thenReturn(expected);

                // Act
                PasswordResetTokenDto result = authService.createPasswordResetToken(email);

                // Assert
                assertSame(expected, result);
                ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
                verify(passwordResetTokenMapper).toEntity(tokenCaptor.capture(), eq(42L));
                assertDoesNotThrow(() -> java.util.UUID.fromString(tokenCaptor.getValue()));
                verify(passwordResetTokenRepository).save(tokenEntity);
                verify(passwordResetTokenMapper).toDto(tokenEntity);
        }

        @Test
        void createPasswordResetToken_shouldThrowWhenEmailIsUnknown() {
                // Arrange
                when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

                // Act & Assert
                assertEquals("Invalid email",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.createPasswordResetToken("missing@example.com"))
                                                .getMessage());
                verifyNoInteractions(passwordResetTokenMapper, passwordResetTokenRepository);
        }

        // validatePasswordResetToken

        @Test
        void validatePasswordResetToken_shouldReturnNormallyForValidUnexpiredToken() {
                // Arrange
                Long tokenId = 5L;
                String tokenValue = "valid-token";
                PasswordResetToken token = mock(PasswordResetToken.class);
                when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
                when(token.getToken()).thenReturn(tokenValue);
                when(token.isExpired()).thenReturn(false);

                // Act & Assert
                assertDoesNotThrow(() -> authService.validatePasswordResetToken(tokenId, tokenValue));

                // Assert
                verify(passwordResetTokenRepository).findById(tokenId);
        }

        @Test
        void validatePasswordResetToken_shouldThrowWhenTokenIdDoesNotExist() {
                // Arrange
                Long tokenId = 5L;
                when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(TokenNotFoundException.class,
                                () -> authService.validatePasswordResetToken(tokenId, "token"));
        }

        @Test
        void validatePasswordResetToken_shouldThrowWhenTokenValueDoesNotMatch() {
                // Arrange
                Long tokenId = 5L;
                PasswordResetToken token = mock(PasswordResetToken.class);
                when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
                when(token.getToken()).thenReturn("stored-token");

                // Act & Assert
                assertEquals("Invalid token",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.validatePasswordResetToken(tokenId, "wrong-token"))
                                                .getMessage());
                verify(token, never()).isExpired();
        }

        @Test
        void validatePasswordResetToken_shouldThrowWhenTokenIsExpired() {
                // Arrange
                Long tokenId = 5L;
                PasswordResetToken token = mock(PasswordResetToken.class);
                when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
                when(token.getToken()).thenReturn("valid-token");
                when(token.isExpired()).thenReturn(true);

                // Act & Assert
                assertEquals("Token has expired",
                                assertThrows(BadCredentialsException.class,
                                                () -> authService.validatePasswordResetToken(tokenId, "valid-token"))
                                                .getMessage());
        }
}
