package com.api.flashlearn.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.api.flashlearn.controllers.AuthController;
import com.api.flashlearn.dtos.*;
import com.api.flashlearn.exceptions.*;
import com.api.flashlearn.services.AuthService;
import com.api.flashlearn.services.JwtService;
import com.api.flashlearn.services.UserService;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {
        @Autowired
        private MockMvc mvc;
        @MockitoBean
        private AuthService authService;
        @MockitoBean
        private UserService userService;

        @MockitoBean
        private JwtService jwtService;

        @Test
        void login_returnsJwt() throws Exception {
                when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                                .thenReturn(new JwtResponse("access.jwt"));
                mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"secret\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("access.jwt"));
                verify(authService).login(any(LoginRequest.class), any(HttpServletResponse.class));
        }

        @Test
        void login_returns401ForBadCredentials() throws Exception {
                when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                                .thenThrow(new BadCredentialsException("Invalid credentials"));
                mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void login_returns403ForUnverifiedAccount() throws Exception {
                when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                                .thenThrow(new AccountNotVerifiedException());
                mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"secret\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void login_rejectsMissingEmailAndPassword() throws Exception {
                mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"\",\"password\":\"\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.email").exists())
                                .andExpect(jsonPath("$.password").exists());
                verifyNoInteractions(authService);
        }

        @Test
        void register_returns201AndLocation() throws Exception {
                when(authService.register(any(RegisterUserRequest.class)))
                                .thenReturn(new UserDto(17L, "new@example.com", "New User", null, null));
                mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"new@example.com","name":"New User","password":"secret"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "http://localhost/users/17"))
                                .andExpect(jsonPath("$.id").value(17));
        }

        @Test
        void register_returns400WhenEmailAlreadyInUse() throws Exception {
                when(authService.register(any(RegisterUserRequest.class))).thenThrow(new EmailInUseException());
                mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"new@example.com","name":"New User","password":"secret"}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Email is already in use"));
        }

        @Test
        void register_validatesRequiredAndConstrainedFields() throws Exception {
                mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"NOT-AN-EMAIL","name":"A","password":""}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.email").exists())
                                .andExpect(jsonPath("$.name").exists())
                                .andExpect(jsonPath("$.password").exists());
                verifyNoInteractions(authService);
        }

        @Test
        void register_rejectsUppercaseEmail() throws Exception {
                mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"User@example.com","name":"User","password":"secret"}"""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.email").exists());
        }

        @Test
        void refresh_returnsNewJwt() throws Exception {
                when(authService.refresh("refresh.jwt")).thenReturn(new JwtResponse("new.access.jwt"));
                mvc.perform(post("/auth/refresh")
                                .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh.jwt")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("new.access.jwt"));
                verify(authService).refresh("refresh.jwt");
        }

        @Test
        void refresh_returns401ForInvalidOrExpiredRefreshToken() throws Exception {
                when(authService.refresh("bad.jwt")).thenReturn(null);
                mvc.perform(post("/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refreshToken", "bad.jwt")))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void refresh_returns400WhenCookieIsMissing() throws Exception {
                mvc.perform(post("/auth/refresh")).andExpect(status().isBadRequest());
        }

        @Test
        void verifyEmailForPasswordReset_returnsToken() throws Exception {
                var token = new PasswordResetTokenDto();
                token.setId(8L);
                token.setToken("reset-token");
                token.setUserId(17L);
                when(authService.createPasswordResetToken("user@example.com")).thenReturn(token);
                mvc.perform(post("/auth/verify-email-for-password-reset").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(8))
                                .andExpect(jsonPath("$.token").value("reset-token"));
        }

        @Test
        void verifyEmailForPasswordReset_returns401ForUnknownEmail() throws Exception {
                when(authService.createPasswordResetToken("user@example.com"))
                                .thenThrow(new BadCredentialsException("Invalid email"));
                mvc.perform(post("/auth/verify-email-for-password-reset").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void verifyEmailForPasswordReset_validatesEmail() throws Exception {
                mvc.perform(post("/auth/verify-email-for-password-reset").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"bad-email\"}"))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(authService);
        }

        @Test
        void resetPassword_returns200AndDelegatesPasswordUpdate() throws Exception {
                mvc.perform(post("/auth/reset-password/17/8").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"token":"valid-token","newPassword":"new-secret","confirmPassword":"new-secret"}
                                                """))
                                .andExpect(status().isOk());
                verify(authService).validatePasswordResetToken(8L, "valid-token");
                verify(userService).setPassword(17L, "new-secret");
        }

        @Test
        void resetPassword_returns400WhenPasswordsDoNotMatch() throws Exception {
                mvc.perform(post("/auth/reset-password/17/8").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"token":"valid-token","newPassword":"new-secret","confirmPassword":"different"}
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
                verifyNoInteractions(userService);
                verify(authService, never()).validatePasswordResetToken(anyLong(), anyString());
        }

        @Test
        void resetPassword_returns404WhenResetTokenDoesNotExist() throws Exception {
                doThrow(new TokenNotFoundException()).when(authService).validatePasswordResetToken(8L, "missing");
                mvc.perform(post("/auth/reset-password/17/8").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"token":"missing","newPassword":"new-secret","confirmPassword":"new-secret"}
                                                """))
                                .andExpect(status().isNotFound());
                verifyNoInteractions(userService);
        }

        @Test
        void resetPassword_returns401WhenTokenIsInvalid() throws Exception {
                doThrow(new BadCredentialsException("Invalid token"))
                                .when(authService).validatePasswordResetToken(8L, "wrong");
                mvc.perform(post("/auth/reset-password/17/8").contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"token":"wrong","newPassword":"new-secret","confirmPassword":"new-secret"}
                                                """))
                                .andExpect(status().isUnauthorized());
                verifyNoInteractions(userService);
        }

        @Test
        void resetPassword_validatesRequiredFields() throws Exception {
                mvc.perform(post("/auth/reset-password/17/8").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"\",\"newPassword\":\"\",\"confirmPassword\":\"\"}"))
                                .andExpect(status().isBadRequest());
                verifyNoInteractions(authService, userService);
        }

        @Test
        void verifyUser_returnsSuccessMessage() throws Exception {
                mvc.perform(post("/auth/verify-user").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"verificationCode\":\"123456\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Account verified successfully"));
                verify(authService).verifyUser(any(VerifyUserRequest.class));
        }

        @Test
        void verifyUser_returns401WhenVerificationFails() throws Exception {
                doThrow(new BadCredentialsException("Invalid verification code"))
                                .when(authService).verifyUser(any(VerifyUserRequest.class));
                mvc.perform(post("/auth/verify-user").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"verificationCode\":\"000000\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void resend_returnsSuccessMessage() throws Exception {
                mvc.perform(post("/auth/resend").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Verification code sent"));
                verify(authService).resendVerificationCode("user@example.com");
        }

        @Test
        void resend_returns401WhenEmailCannotBeResent() throws Exception {
                doThrow(new BadCredentialsException("Invalid email"))
                                .when(authService).resendVerificationCode("missing@example.com");
                mvc.perform(post("/auth/resend").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"missing@example.com\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void logout_returns204AndExpiresRefreshCookie() throws Exception {
                mvc.perform(post("/auth/logout"))
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().exists("refreshToken"))
                                .andExpect(cookie().maxAge("refreshToken", 0))
                                .andExpect(cookie().httpOnly("refreshToken", true))
                                .andExpect(cookie().secure("refreshToken", true))
                                .andExpect(cookie().path("refreshToken", "/auth/refresh"));
        }

        @Test
        void unhandledRuntimeException_isMappedTo400ByGlobalHandler() throws Exception {
                doThrow(new IllegalStateException("Unexpected failure"))
                                .when(authService).verifyUser(any(VerifyUserRequest.class));
                mvc.perform(post("/auth/verify-user").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"verificationCode\":\"123456\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Unexpected failure"));
        }
}
