package com.orang.authservice.service;

import com.orang.shared.exception.EmailServiceException;
import com.orang.shared.exception.EmailServiceException.EmailErrorType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.net.SocketTimeoutException;
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
            log.info("Email sent successfully to {}: {}", to, subject);

        } catch (MailSendException e) {
            handleMailSendException(to, subject, e);
        } catch (MessagingException e) {
            handleMessagingException(to, subject, e);
        } catch (TemplateEngineException e) {
            log.error("Email template processing failed for template: {}", templateName, e);
            throw new EmailServiceException(
                    "Failed to process email template: " + e.getMessage(),
                    EmailErrorType.TEMPLATE_ERROR,
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {} with subject {}", to, subject, e);
            throw new EmailServiceException(
                    "Failed to send email: " + e.getMessage(),
                    EmailErrorType.UNKNOWN,
                    e
            );
        }
    }

    /**
     * Handles MailSendException which wraps mail server connection and authentication errors.
     * Examines the root cause to determine the specific error type.
     */
    private void handleMailSendException(String to, String subject, MailSendException e) {
        log.warn("Mail send exception occurred for email to {}: {}", to, e.getMessage());

        // Check for socket timeout (MailTrap connection timeout)
        if (e.contains(SocketTimeoutException.class)) {
            log.error("Mail server connection timed out. MailTrap is not responding. Recipient: {}", to);
            throw new EmailServiceException(
                    "Email service is temporarily unavailable due to mail server timeout. Please try again later.",
                    EmailErrorType.TIMEOUT,
                    e
            );
        }

        // Check for MessagingException (TLS conversion failure, auth failure, etc.)
        if (e.contains(MessagingException.class)) {
            Throwable rootCause = e.getMostSpecificCause();
            String rootCauseMsg = rootCause != null ? rootCause.getMessage() : e.getMessage();
            String fullMsg = e.getMessage() + " " + (rootCauseMsg != null ? rootCauseMsg : "");

            // Check for email limit (MailTrap quota exceeded)
            if (fullMsg.contains("email limit") || fullMsg.contains("upgrade your plan")) {
                log.error("MailTrap email limit reached. Account needs upgrade. Recipient: {}", to);
                throw new EmailServiceException(
                        "Email service has reached its usage limit. Please try again later or contact support.",
                        EmailErrorType.AUTHENTICATION_FAILED,
                        e
                );
            }

            // Check for TLS handshake failure
            if (rootCauseMsg != null && rootCauseMsg.contains("Could not convert socket to TLS")) {
                log.error("TLS handshake failed with MailTrap. Possible certificate or configuration issue. Recipient: {}", to);
                throw new EmailServiceException(
                        "Email service is temporarily unavailable due to secure connection issues. Please try again later.",
                        EmailErrorType.CONNECTION_FAILED,
                        e
                );
            }

            // Check for general authentication failure
            if (rootCauseMsg != null && rootCauseMsg.toLowerCase().contains("authentication")) {
                log.error("MailTrap authentication failed. Check credentials or account status. Error: {}", rootCauseMsg);
                throw new EmailServiceException(
                        "Email service is experiencing authentication issues. Please try again later.",
                        EmailErrorType.AUTHENTICATION_FAILED,
                        e
                );
            }
        }

        // Generic mail send failure
        log.error("Mail server connection failed. Error: {}", e.getMessage());
        throw new EmailServiceException(
                "Email service is temporarily unavailable. Please try again later.",
                EmailErrorType.CONNECTION_FAILED,
                e
        );
    }

    /**
     * Handles MessagingException from Jakarta Mail API.
     * This can occur during message construction or address validation.
     */
    private void handleMessagingException(String to, String subject, MessagingException e) {
        if (e instanceof AddressException) {
            log.warn("Invalid email address: {}", to);
            throw new EmailServiceException(
                    "Invalid email address provided",
                    EmailErrorType.INVALID_EMAIL,
                    e
            );
        }

        // Check for socket timeout in the root cause chain
        Throwable rootCause = e.getCause();
        while (rootCause != null) {
            if (rootCause instanceof SocketTimeoutException) {
                log.error("Mail server connection timed out (from MessagingException). Recipient: {}", to);
                throw new EmailServiceException(
                        "Email service is temporarily unavailable due to mail server timeout. Please try again later.",
                        EmailErrorType.TIMEOUT,
                        e
                );
            }
            rootCause = rootCause.getCause();
        }

        log.error("Messaging error while sending email to {} with subject {}: {}", to, subject, e.getMessage());
        throw new EmailServiceException(
                "Failed to send email: " + e.getMessage(),
                EmailErrorType.UNKNOWN,
                e
        );
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
