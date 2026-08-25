package com.saumya.async_notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendEmail(
            String email,
            String subject,
            String message) {

        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom(senderEmail);
        mail.setTo(email);
        mail.setSubject(subject);
        mail.setText(message);

        mailSender.send(mail);

        log.info("Email submitted successfully for recipient={}", email);
    }
}