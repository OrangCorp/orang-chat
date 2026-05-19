package com.orang.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.setMethod("GET");
    }

    @Test
    @DisplayName("base exception response keeps status and message")
    void handleBaseException() {
        BaseException ex = new BaseException("bad request", HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorResponse> response = handler.handleBaseException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("bad request");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("email service exception uses service unavailable")
    void handleEmailServiceException() {
        EmailServiceException ex = new EmailServiceException("mail down", EmailServiceException.EmailErrorType.TIMEOUT);

        ResponseEntity<ErrorResponse> response = handler.handleEmailServiceException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("mail down");
    }

    @Test
    @DisplayName("validation exception returns field errors")
    void handleValidationException() {
        DataBinder binder = new DataBinder(new Object(), "payload");
        binder.getBindingResult().addError(new FieldError("payload", "email", "Invalid email"));
        binder.getBindingResult().addError(new FieldError("payload", "password", "Too short"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binder.getBindingResult());

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors())
                .containsEntry("email", "Invalid email")
                .containsEntry("password", "Too short");
    }

    @Test
    @DisplayName("no resource found returns 404 with request path")
    void handleNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/missing");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("/api/test");
    }

    @Test
    @DisplayName("no handler found returns 404 with endpoint details")
    void handleNoHandlerFound() {
        NoHandlerFoundException ex = new NoHandlerFoundException("POST", "/unknown", null);

        ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("POST /unknown");
    }

    @Test
    @DisplayName("method not supported includes supported methods")
    void handleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("PATCH");
        when(ex.getSupportedHttpMethods()).thenReturn(Set.of(HttpMethod.GET, HttpMethod.POST));

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("PATCH").contains("GET").contains("POST");
    }

    @Test
    @DisplayName("method not supported falls back to unknown when supported methods are missing")
    void handleMethodNotSupported_WithMissingSupportedMethods_UsesUnknownFallback() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("PATCH");
        when(ex.getSupportedHttpMethods()).thenReturn(null);

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Unknown");
    }

    @Test
    @DisplayName("generic exception returns 500 and safe message")
    void handleGenericException() {
        RuntimeException ex = new RuntimeException("unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }
}
