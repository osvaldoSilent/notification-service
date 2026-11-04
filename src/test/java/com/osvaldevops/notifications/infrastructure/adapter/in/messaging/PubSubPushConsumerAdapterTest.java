package com.osvaldevops.notifications.infrastructure.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.exception.InvalidPubSubMessageException;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.PubSubPushRequest;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.PubSubPushRequest.PubSubMessage;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper.UserCreatedEventMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link PubSubPushConsumerAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class PubSubPushConsumerAdapterTest {

    @Mock
    private SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;

    @Mock
    private UserCreatedEventMapper userCreatedEventMapper;

    private ObjectMapper objectMapper;
    private PubSubPushConsumerAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new PubSubPushConsumerAdapter(sendWelcomeNotificationUseCase, userCreatedEventMapper, objectMapper);
    }

    @Test
    @DisplayName("Should decode Base64 payload, route USER_CREATED event, and return 204 No Content")
    void should_ProcessUserCreatedEventAndReturnNoContent_When_PushMessageIsValid() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-04T23:30:00Z");
        UserCreatedMessagePayload payload = new UserCreatedMessagePayload(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );
        UserCreatedEvent domainEvent = new UserCreatedEvent(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );

        String json = objectMapper.writeValueAsString(payload);
        String base64Data = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        PubSubMessage message = new PubSubMessage(
                base64Data,
                "msg-101",
                "2026-11-04T23:30:01Z",
                Map.of("eventType", "USER_CREATED")
        );
        PubSubPushRequest pushRequest = new PubSubPushRequest(
                message,
                "projects/osvaldevops/subscriptions/user-created-events-sub"
        );

        when(userCreatedEventMapper.toDomainEvent(payload)).thenReturn(domainEvent);

        // When
        ResponseEntity<Void> response = adapter.onPubSubPushMessage(pushRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userCreatedEventMapper).toDomainEvent(payload);
        verify(sendWelcomeNotificationUseCase).sendWelcomeNotification(domainEvent);
    }

    @Test
    @DisplayName("Should default to USER_CREATED eventType when attributes are null, empty, or blank")
    void should_DefaultToUserCreatedEvent_When_AttributesAreMissingOrBlank() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-04T23:30:00Z");
        UserCreatedMessagePayload payload = new UserCreatedMessagePayload(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );
        UserCreatedEvent domainEvent = new UserCreatedEvent(
                userId,
                "osval@osvaldevops.com",
                "osval",
                "ROLE_USER",
                now
        );

        String base64Data = Base64.getEncoder().encodeToString(
                objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8)
        );

        PubSubPushRequest nullAttrsRequest = new PubSubPushRequest(
                new PubSubMessage(base64Data, "msg-1", "2026-11-04T23:30:01Z", null),
                "sub-1"
        );
        PubSubPushRequest emptyAttrsRequest = new PubSubPushRequest(
                new PubSubMessage(base64Data, "msg-2", "2026-11-04T23:30:01Z", Collections.emptyMap()),
                "sub-1"
        );
        PubSubPushRequest blankEventTypeRequest = new PubSubPushRequest(
                new PubSubMessage(base64Data, "msg-3", "2026-11-04T23:30:01Z", Map.of("eventType", "   ")),
                "sub-1"
        );
        PubSubPushRequest nullEventTypeValueRequest = new PubSubPushRequest(
                new PubSubMessage(base64Data, "msg-4", "2026-11-04T23:30:01Z", Collections.singletonMap("eventType", null)),
                "sub-1"
        );

        when(userCreatedEventMapper.toDomainEvent(payload)).thenReturn(domainEvent);

        // When
        ResponseEntity<Void> res1 = adapter.onPubSubPushMessage(nullAttrsRequest);
        ResponseEntity<Void> res2 = adapter.onPubSubPushMessage(emptyAttrsRequest);
        ResponseEntity<Void> res3 = adapter.onPubSubPushMessage(blankEventTypeRequest);
        ResponseEntity<Void> res4 = adapter.onPubSubPushMessage(nullEventTypeValueRequest);

        // Then
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(res3.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(res4.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should throw InvalidPubSubMessageException when push request or message is null or data is blank")
    void should_ThrowInvalidPubSubMessageException_When_RequestOrDataIsMissing() {
        // Given
        PubSubPushRequest nullMessageRequest = new PubSubPushRequest(null, "sub");
        PubSubPushRequest nullDataRequest = new PubSubPushRequest(
                new PubSubMessage(null, "msg-1", "2026-11-04T23:30:00Z", null),
                "sub"
        );
        PubSubPushRequest blankDataRequest = new PubSubPushRequest(
                new PubSubMessage("   ", "msg-2", "2026-11-04T23:30:00Z", null),
                "sub"
        );

        // When / Then
        assertThatThrownBy(() -> adapter.onPubSubPushMessage(null))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Pub/Sub push request and message envelope must not be null");

        assertThatThrownBy(() -> adapter.onPubSubPushMessage(nullMessageRequest))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Pub/Sub push request and message envelope must not be null");

        assertThatThrownBy(() -> adapter.onPubSubPushMessage(nullDataRequest))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Pub/Sub message data must not be null or blank");

        assertThatThrownBy(() -> adapter.onPubSubPushMessage(blankDataRequest))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Pub/Sub message data must not be null or blank");

        verify(sendWelcomeNotificationUseCase, never()).sendWelcomeNotification(any());
    }

    @Test
    @DisplayName("Should throw InvalidPubSubMessageException when Base64 data is invalid")
    void should_ThrowInvalidPubSubMessageException_When_Base64DataIsInvalid() {
        // Given
        PubSubPushRequest invalidBase64Request = new PubSubPushRequest(
                new PubSubMessage("%%%not-valid-base64%%%", "msg-1", "2026-11-04T23:30:00Z", null),
                "sub"
        );

        // When / Then
        assertThatThrownBy(() -> adapter.onPubSubPushMessage(invalidBase64Request))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Failed to decode Base64 Pub/Sub message data")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw InvalidPubSubMessageException when decoded JSON is malformed")
    void should_ThrowInvalidPubSubMessageException_When_DecodedJsonIsMalformed() {
        // Given
        String malformedBase64 = Base64.getEncoder().encodeToString(
                "{malformed-json".getBytes(StandardCharsets.UTF_8)
        );
        PubSubPushRequest malformedJsonRequest = new PubSubPushRequest(
                new PubSubMessage(malformedBase64, "msg-1", "2026-11-04T23:30:00Z", Map.of("eventType", "USER_CREATED")),
                "sub"
        );

        // When / Then
        assertThatThrownBy(() -> adapter.onPubSubPushMessage(malformedJsonRequest))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Malformed JSON payload for USER_CREATED event");
    }

    @Test
    @DisplayName("Should throw InvalidPubSubMessageException when eventType is unsupported")
    void should_ThrowInvalidPubSubMessageException_When_EventTypeIsUnsupported() {
        // Given
        String validBase64 = Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        PubSubPushRequest unsupportedEventRequest = new PubSubPushRequest(
                new PubSubMessage(validBase64, "msg-1", "2026-11-04T23:30:00Z", Map.of("eventType", "UNKNOWN_EVENT")),
                "sub"
        );

        // When / Then
        assertThatThrownBy(() -> adapter.onPubSubPushMessage(unsupportedEventRequest))
                .isInstanceOf(InvalidPubSubMessageException.class)
                .hasMessage("Unsupported Pub/Sub eventType: UNKNOWN_EVENT");
    }
}

