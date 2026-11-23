package com.api.flashlearn.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import com.api.flashlearn.services.EmailService;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendEmail_shouldCreateAndSendHtmlEmail() throws Exception {
        // Arrange
        String to = "recipient@example.com";
        String subject = "Test subject";
        String body = "<h1>Hello</h1>";
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        // Act
        emailService.sendEmail(to, subject, body);

        // Assert
        message.saveChanges();
        assertArrayEquals(new String[] { to }, message.getRecipients(jakarta.mail.Message.RecipientType.TO));
        assertEquals(subject, message.getSubject());
        assertTrue(message.getContentType().toLowerCase().contains("multipart"));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(message);
    }

    @Test
    void sendEmail_shouldPropagateMessagingExceptionWhenMessageCannotBeCreated() {
        // Arrange
        when(mailSender.createMimeMessage())
                .thenThrow(new org.springframework.mail.MailPreparationException("creation failed"));

        // Act & Assert
        assertThrows(org.springframework.mail.MailPreparationException.class,
                () -> emailService.sendEmail("to@example.com", "subject", "<p>body</p>"));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
