package com.orang.authservice.service;

import com.orang.shared.exception.EmailServiceException;
import com.orang.shared.exception.EmailServiceException.EmailErrorType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.IContext;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private SpringTemplateEngine templateEngine;
    private EmailService emailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(SpringTemplateEngine.class);
        emailService = new EmailService(mailSender, templateEngine);
        mimeMessage = new MimeMessage((jakarta.mail.Session) null);

        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@orang.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Orang Chat");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<p>Hello</p>");
    }

    @Test
    @DisplayName("sendEmail sends rendered HTML content")
    void sendEmail_Success() {
        emailService.sendEmail("user@example.com", "Subject", "template", Map.of("name", "User"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail maps socket timeout mail send failures")
    void sendEmail_MailSendTimeout_MapsToTimeoutError() {
        MailSendException timeoutException = mock(MailSendException.class);
        when(timeoutException.contains(SocketTimeoutException.class)).thenReturn(true);
        doThrow(timeoutException).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.TIMEOUT);
    }

    @Test
    @DisplayName("sendEmail maps MailTrap limit failures to authentication error")
    void sendEmail_MailLimit_MapsToAuthenticationFailed() {
        MessagingException root = new MessagingException("Please upgrade your plan, email limit exceeded");
        MailSendException mailSendException = mock(MailSendException.class);
        when(mailSendException.contains(SocketTimeoutException.class)).thenReturn(false);
        when(mailSendException.contains(MessagingException.class)).thenReturn(true);
        when(mailSendException.getMostSpecificCause()).thenReturn(root);
        when(mailSendException.getMessage()).thenReturn("send failed");
        doThrow(mailSendException).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("sendEmail maps TLS handshake failures to connection error")
    void sendEmail_TlsFailure_MapsToConnectionFailed() {
        MessagingException root = new MessagingException("Could not convert socket to TLS");
        MailSendException mailSendException = mock(MailSendException.class);
        when(mailSendException.contains(SocketTimeoutException.class)).thenReturn(false);
        when(mailSendException.contains(MessagingException.class)).thenReturn(true);
        when(mailSendException.getMostSpecificCause()).thenReturn(root);
        when(mailSendException.getMessage()).thenReturn("send failed");
        doThrow(mailSendException).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.CONNECTION_FAILED);
    }

    @Test
    @DisplayName("sendEmail maps messaging auth failures to authentication error")
    void sendEmail_AuthenticationFailure_MapsToAuthenticationFailed() {
        MessagingException root = new MessagingException("Authentication failed");
        MailSendException mailSendException = mock(MailSendException.class);
        when(mailSendException.contains(SocketTimeoutException.class)).thenReturn(false);
        when(mailSendException.contains(MessagingException.class)).thenReturn(true);
        when(mailSendException.getMostSpecificCause()).thenReturn(root);
        when(mailSendException.getMessage()).thenReturn("send failed");
        doThrow(mailSendException).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("sendEmail falls back to connection failure for unknown mail send errors")
    void sendEmail_GenericMailSend_MapsToConnectionFailed() {
        MailSendException mailSendException = mock(MailSendException.class);
        when(mailSendException.contains(SocketTimeoutException.class)).thenReturn(false);
        when(mailSendException.contains(MessagingException.class)).thenReturn(false);
        when(mailSendException.getMessage()).thenReturn("generic");
        doThrow(mailSendException).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.CONNECTION_FAILED);
    }

    @DisplayName("sendEmail maps template engine errors")
    void sendEmail_TemplateFailure_MapsToTemplateError() {
        when(templateEngine.process(eq("template"), any(IContext.class)))
                .thenThrow(new TemplateInputException("template failure"));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.TEMPLATE_ERROR);
    }

    @Test
    @DisplayName("sendEmail maps unexpected runtime errors")
    void sendEmail_UnexpectedError_MapsToUnknown() {
        when(templateEngine.process(eq("template"), any(IContext.class)))
                .thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "template", Map.of()))
                .isInstanceOf(EmailServiceException.class)
                .extracting(ex -> ((EmailServiceException) ex).getErrorType())
                .isEqualTo(EmailErrorType.UNKNOWN);
    }

    @Test
    @DisplayName("sendVerificationEmail delegates to template with expected data")
    void sendVerificationEmail_UsesExpectedTemplate() {
        emailService.sendVerificationEmail("verify@example.com", "Alex", "123456");

        verify(templateEngine).process(eq("email/verification-code"), any(IContext.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetEmail delegates to template with expected data")
    void sendPasswordResetEmail_UsesExpectedTemplate() {
        emailService.sendPasswordResetEmail("reset@example.com", "Alex", "https://app/reset");

        verify(templateEngine).process(eq("email/password-reset"), any(IContext.class));
        verify(mailSender).send(any(MimeMessage.class));
    }
}
