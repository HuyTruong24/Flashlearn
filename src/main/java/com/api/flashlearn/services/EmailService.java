package com.api.flashlearn.services;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class EmailService {
    private JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) throws MessagingException{
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true); // true enables html content, attachments, etc.

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // true indicates HTML

        mailSender.send(message);
    }
}
