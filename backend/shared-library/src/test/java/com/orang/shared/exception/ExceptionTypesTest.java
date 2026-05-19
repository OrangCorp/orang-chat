package com.orang.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTypesTest {

    @Test
    @DisplayName("base exception stores message and status")
    void baseException_StoresFields() {
        BaseException ex = new BaseException("boom", HttpStatus.BAD_REQUEST);

        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("domain exceptions map to expected statuses")
    void domainExceptions_StatusMapping() {
        assertThat(new BadRequestException("bad").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new ResourceNotFoundException("missing").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new ForbiddenException("forbidden").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new UnauthorizedException("unauthorized").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new TooManyRequestsException("limit").getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("conflict exception keeps message")
    void conflictException_Message() {
        ConflictException ex = new ConflictException("conflict");
        assertThat(ex.getMessage()).isEqualTo("conflict");
    }

    @Test
    @DisplayName("email service exception stores type and cause")
    void emailServiceException_StoresTypeAndCause() {
        RuntimeException cause = new RuntimeException("mail down");
        EmailServiceException ex = new EmailServiceException("failed", EmailServiceException.EmailErrorType.TIMEOUT, cause);

        assertThat(ex.getMessage()).isEqualTo("failed");
        assertThat(ex.getErrorType()).isEqualTo(EmailServiceException.EmailErrorType.TIMEOUT);
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("error response builder stores all fields")
    void errorResponseBuilder_StoresAllFields() {
        LocalDateTime timestamp = LocalDateTime.now();

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("invalid payload")
                .path("/api/test")
                .validationErrors(Map.of("email", "invalid"))
                .build();

        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getError()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
        assertThat(response.getMessage()).isEqualTo("invalid payload");
        assertThat(response.getPath()).isEqualTo("/api/test");
        assertThat(response.getValidationErrors()).containsEntry("email", "invalid");
    }
}
