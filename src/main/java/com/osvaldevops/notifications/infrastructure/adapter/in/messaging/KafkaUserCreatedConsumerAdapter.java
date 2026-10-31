package com.osvaldevops.notifications.infrastructure.adapter.in.messaging;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper.UserCreatedEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound Kafka listener adapter consuming {@link UserCreatedMessagePayload} events
 * published by {@code login-service} and triggering the welcome notification use case.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserCreatedConsumerAdapter {

    private final SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;
    private final UserCreatedEventMapper userCreatedEventMapper;

    /**
     * Consumes user creation events from the configured Kafka topic and triggers
     * a welcome notification.
     *
     * @param payload deserialized Kafka message payload
     */
    @KafkaListener(
            topics = "${app.kafka.topics.user-created:user-created-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
    )
    public void onUserCreated(UserCreatedMessagePayload payload) {
        log.info("Received UserCreatedEvent from Kafka for userId={} email={}",
                payload.userId(), payload.email());

        UserCreatedEvent event = userCreatedEventMapper.toDomainEvent(payload);
        sendWelcomeNotificationUseCase.sendWelcomeNotification(event);
    }
}

