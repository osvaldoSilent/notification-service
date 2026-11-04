package com.osvaldevops.notifications.infrastructure.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper.UserCreatedEventMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PubSubUserCreatedSubscriberAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class PubSubUserCreatedSubscriberAdapterTest {

    private static final String SUBSCRIPTION = "user-created-sub";

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;

    @Mock
    private UserCreatedEventMapper userCreatedEventMapper;

    private ObjectMapper objectMapper;
    private PubSubUserCreatedSubscriberAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new PubSubUserCreatedSubscriberAdapter(
                pubSubTemplate,
                sendWelcomeNotificationUseCase,
                userCreatedEventMapper,
                objectMapper,
                SUBSCRIPTION,
                true
        );
    }

    @Test
    @DisplayName("Should subscribe to GCP Pub/Sub subscription when subscriberEnabled is true")
    @SuppressWarnings("unchecked")
    void should_SubscribeToPubSub_When_SubscriberEnabledIsTrue() {
        // Given / When
        adapter.startSubscription();

        // Then
        verify(pubSubTemplate).subscribe(eq(SUBSCRIPTION), any(Consumer.class));
    }

    @Test
    @DisplayName("Should not subscribe to GCP Pub/Sub when subscriberEnabled is false")
    @SuppressWarnings("unchecked")
    void should_NotSubscribe_When_SubscriberEnabledIsFalse() {
        // Given
        PubSubUserCreatedSubscriberAdapter disabledAdapter = new PubSubUserCreatedSubscriberAdapter(
                pubSubTemplate,
                sendWelcomeNotificationUseCase,
                userCreatedEventMapper,
                objectMapper,
                SUBSCRIPTION,
                false
        );

        // When
        disabledAdapter.startSubscription();

        // Then
        verify(pubSubTemplate, never()).subscribe(any(), any(Consumer.class));
    }

    @Test
    @DisplayName("Should process USER_CREATED message, invoke use case, and ACK message")
    void should_ProcessMessageAndAck_When_ValidUserCreatedEventReceived() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-05T02:30:00Z");
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
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setMessageId("msg-100")
                .setData(ByteString.copyFromUtf8(json))
                .putAttributes("eventType", "USER_CREATED")
                .build();

        BasicAcknowledgeablePubsubMessage ackMessage = mock(BasicAcknowledgeablePubsubMessage.class);
        when(ackMessage.getPubsubMessage()).thenReturn(pubsubMessage);
        when(userCreatedEventMapper.toDomainEvent(payload)).thenReturn(domainEvent);

        // When
        adapter.handleMessage(ackMessage);

        // Then
        verify(userCreatedEventMapper).toDomainEvent(payload);
        verify(sendWelcomeNotificationUseCase).sendWelcomeNotification(domainEvent);
        verify(ackMessage).ack();
        verify(ackMessage, never()).nack();
    }

    @Test
    @DisplayName("Should default to USER_CREATED when attributes are missing or eventType is blank")
    void should_DefaultToUserCreated_When_EventTypeAttributeIsMissingOrBlank() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-05T02:30:00Z");
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

        PubsubMessage noAttrMsg = PubsubMessage.newBuilder()
                .setMessageId("msg-1")
                .setData(ByteString.copyFromUtf8(json))
                .build();
        PubsubMessage blankAttrMsg = PubsubMessage.newBuilder()
                .setMessageId("msg-2")
                .setData(ByteString.copyFromUtf8(json))
                .putAttributes("eventType", "   ")
                .build();

        BasicAcknowledgeablePubsubMessage ackMsg1 = mock(BasicAcknowledgeablePubsubMessage.class);
        BasicAcknowledgeablePubsubMessage ackMsg2 = mock(BasicAcknowledgeablePubsubMessage.class);
        BasicAcknowledgeablePubsubMessage ackMsgNullMap = mock(BasicAcknowledgeablePubsubMessage.class);
        PubsubMessage mockPubsubWithNullMap = mock(PubsubMessage.class);
        when(mockPubsubWithNullMap.getMessageId()).thenReturn("msg-3");
        when(mockPubsubWithNullMap.getData()).thenReturn(ByteString.copyFromUtf8(json));
        when(mockPubsubWithNullMap.getAttributesMap()).thenReturn(null);

        BasicAcknowledgeablePubsubMessage ackMsgNullValue = mock(BasicAcknowledgeablePubsubMessage.class);
        PubsubMessage mockPubsubWithNullVal = mock(PubsubMessage.class);
        when(mockPubsubWithNullVal.getMessageId()).thenReturn("msg-4");
        when(mockPubsubWithNullVal.getData()).thenReturn(ByteString.copyFromUtf8(json));
        when(mockPubsubWithNullVal.getAttributesMap()).thenReturn(Collections.singletonMap("eventType", null));

        when(ackMsg1.getPubsubMessage()).thenReturn(noAttrMsg);
        when(ackMsg2.getPubsubMessage()).thenReturn(blankAttrMsg);
        when(ackMsgNullMap.getPubsubMessage()).thenReturn(mockPubsubWithNullMap);
        when(ackMsgNullValue.getPubsubMessage()).thenReturn(mockPubsubWithNullVal);
        when(userCreatedEventMapper.toDomainEvent(payload)).thenReturn(domainEvent);

        // When
        adapter.handleMessage(ackMsg1);
        adapter.handleMessage(ackMsg2);
        adapter.handleMessage(ackMsgNullMap);
        adapter.handleMessage(ackMsgNullValue);

        // Then
        verify(ackMsg1).ack();
        verify(ackMsg2).ack();
        verify(ackMsgNullMap).ack();
        verify(ackMsgNullValue).ack();
    }

    @Test
    @DisplayName("Should ACK and ignore message when eventType is unsupported")
    void should_AckAndIgnore_When_EventTypeIsUnsupported() {
        // Given
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setMessageId("msg-unsupported")
                .setData(ByteString.copyFromUtf8("{}"))
                .putAttributes("eventType", "UNKNOWN_EVENT")
                .build();
        BasicAcknowledgeablePubsubMessage ackMessage = mock(BasicAcknowledgeablePubsubMessage.class);
        when(ackMessage.getPubsubMessage()).thenReturn(pubsubMessage);

        // When
        adapter.handleMessage(ackMessage);

        // Then
        verify(sendWelcomeNotificationUseCase, never()).sendWelcomeNotification(any());
        verify(ackMessage).ack();
    }

    @Test
    @DisplayName("Should NACK message when NotificationDeliveryException occurs so Pub/Sub retries")
    void should_NackMessage_When_NotificationDeliveryExceptionThrown() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-05T02:30:00Z");
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

        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setMessageId("msg-retry")
                .setData(ByteString.copyFromUtf8(json))
                .putAttributes("eventType", "USER_CREATED")
                .build();
        BasicAcknowledgeablePubsubMessage ackMessage = mock(BasicAcknowledgeablePubsubMessage.class);
        when(ackMessage.getPubsubMessage()).thenReturn(pubsubMessage);
        when(userCreatedEventMapper.toDomainEvent(payload)).thenReturn(domainEvent);
        doThrow(new NotificationDeliveryException("SMTP down"))
                .when(sendWelcomeNotificationUseCase).sendWelcomeNotification(domainEvent);

        // When
        adapter.handleMessage(ackMessage);

        // Then
        verify(ackMessage).nack();
        verify(ackMessage, never()).ack();
    }

    @Test
    @DisplayName("Should ACK poison-pill message when JSON is malformed")
    void should_AckPoisonPillMessage_When_JsonIsMalformed() {
        // Given
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setMessageId("msg-bad-json")
                .setData(ByteString.copyFromUtf8("{bad-json"))
                .putAttributes("eventType", "USER_CREATED")
                .build();
        BasicAcknowledgeablePubsubMessage ackMessage = mock(BasicAcknowledgeablePubsubMessage.class);
        when(ackMessage.getPubsubMessage()).thenReturn(pubsubMessage);

        // When
        adapter.handleMessage(ackMessage);

        // Then
        verify(ackMessage).ack();
        verify(ackMessage, never()).nack();
    }
}

