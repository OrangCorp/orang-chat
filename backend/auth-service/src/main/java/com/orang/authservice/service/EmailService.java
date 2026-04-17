package com.orang.authservice.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public void sendEmail(String to, String subject, String templateName,
                          Map<String, Object> variables) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(to);
            helper.setSubject(subject);

            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Email sent to {}: {}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email to {} with subject {}", to, subject, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendVerificationEmail(String to, String displayName, String code) {
        Map<String, Object> variables = Map.of(
                "displayName", displayName,
                "code", code
        );
        sendEmail(to, "Verify your email — Orang Chat", "email/verification-code", variables);
    }

    public void sendPasswordResetEmail(String to, String displayName, String resetUrl) {
        Map<String, Object> variables = Map.of(
                "displayName", displayName,
                "resetUrl", resetUrl
        );
        sendEmail(to, "Reset your password — Orang Chat", "email/password-reset", variables);
    }
}
