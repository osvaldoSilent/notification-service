package com.osvaldevops.notifications.infrastructure.adapter.in.web.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/notifications/welcome");
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST for IllegalArgumentException")
    void should_ReturnBadRequest_When_IllegalArgumentExceptionThrown() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Recipient email must not be null or blank");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalArgument(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(response.getBody()).status()).isEqualTo(400);
        assertThat(java.util.Objects.requireNonNull(response.getBody()).message())
            .isEqualTo("Recipient email must not be null or blank");
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST with field errors for MethodArgumentNotValidException")
    void should_ReturnBadRequest_When_ValidationFails() {
        // Given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("sendNotificationRequest", "email", "Email must be valid");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(response.getBody()).validationErrors())
            .containsEntry("email", "Email must be valid");
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST for HttpMessageNotReadableException")
    void should_ReturnBadRequest_When_JsonIsMalformed() {
        // Given
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMalformedJson(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(response.getBody()).message())
            .isEqualTo("Malformed JSON request body");
    }

    @Test
    @DisplayName("Should return 503 SERVICE_UNAVAILABLE for NotificationDeliveryException")
    void should_ReturnServiceUnavailable_When_NotificationDeliveryExceptionThrown() {
        // Given
        NotificationDeliveryException ex = new NotificationDeliveryException("SMTP server unreachable", new RuntimeException());

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNotificationDeliveryException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(response.getBody()).status()).isEqualTo(503);
    }

    @Test
    @DisplayName("Should return 500 INTERNAL_SERVER_ERROR for unexpected Exception")
    void should_ReturnInternalServerError_When_UnexpectedExceptionThrown() {
        // Given
        Exception ex = new RuntimeException("Unexpected failure");

        // When
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUnexpectedException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(response.getBody()).status()).isEqualTo(500);
    }
}

