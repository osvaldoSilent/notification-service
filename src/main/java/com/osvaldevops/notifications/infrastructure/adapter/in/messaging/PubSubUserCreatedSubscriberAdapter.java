package com.osvaldevops.notifications.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper.UserCreatedEventMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Inbound Google Cloud Pub/Sub Pull Subscriber adapter that listens to the configured
 * subscription (e.g., {@code user-created-sub}) and triggers welcome notifications.
 */
@Slf4j
@Component
public class PubSubUserCreatedSubscriberAdapter {

    private static final String ATTR_EVENT_TYPE = "eventType";
    private static final String EVENT_USER_CREATED = "USER_CREATED";

    private final PubSubTemplate pubSubTemplate;
    private final SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;
    private final UserCreatedEventMapper userCreatedEventMapper;
    private final ObjectMapper objectMapper;
    private final String subscriptionName;
    private final boolean subscriberEnabled;

    public PubSubUserCreatedSubscriberAdapter(
            PubSubTemplate pubSubTemplate,
            SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase,
            UserCreatedEventMapper userCreatedEventMapper,
            ObjectMapper objectMapper,
            @Value("${app.pubsub.subscriptions.user-created}") String subscriptionName,
            @Value("${app.pubsub.subscriber-enabled}") boolean subscriberEnabled
    ) {
        this.pubSubTemplate = pubSubTemplate;
        this.sendWelcomeNotificationUseCase = sendWelcomeNotificationUseCase;
        this.userCreatedEventMapper = userCreatedEventMapper;
        this.objectMapper = objectMapper;
        this.subscriptionName = subscriptionName;
        this.subscriberEnabled = subscriberEnabled;
    }

    /**
     * Starts listening to the configured GCP Pub/Sub subscription once the application context is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startSubscription() {
        if (!subscriberEnabled) {
            log.info("GCP Pub/Sub pull subscriber is disabled (app.pubsub.subscriber-enabled=false)");
            return;
        }

        log.info("Subscribing to GCP Pub/Sub subscription={}", subscriptionName);
        pubSubTemplate.subscribe(subscriptionName, this::handleMessage);
    }

    /**
     * Processes a single message pulled from the GCP Pub/Sub subscription and acknowledges or NACKs it.
     *
     * @param message acknowledgeable Pub/Sub message wrapper
     */
    public void handleMessage(BasicAcknowledgeablePubsubMessage message) {
        String messageId = message.getPubsubMessage().getMessageId();
        String payloadJson = message.getPubsubMessage().getData().toStringUtf8();
        Map<String, String> attributes = message.getPubsubMessage().getAttributesMap();
        String eventType = resolveEventType(attributes);

        log.info("Received message from GCP Pub/Sub subscription={} messageId={} eventType={}",
                subscriptionName, messageId, eventType);

        try {
            if (EVENT_USER_CREATED.equalsIgnoreCase(eventType)) {
                UserCreatedMessagePayload payload = objectMapper.readValue(payloadJson, UserCreatedMessagePayload.class);
                UserCreatedEvent event = userCreatedEventMapper.toDomainEvent(payload);
                sendWelcomeNotificationUseCase.sendWelcomeNotification(event);
                message.ack();
                log.info("Processed and ACKed GCP Pub/Sub messageId={} for email={}", messageId, payload.email());
            } else {
                log.warn("Ignoring unsupported GCP Pub/Sub eventType={} for messageId={}; acknowledging message",
                        eventType, messageId);
                message.ack();
            }
        } catch (NotificationDeliveryException ex) {
            log.error("Transient delivery failure for messageId={}; sending NACK for retry", messageId, ex);
            message.nack();
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.error("Invalid payload in GCP Pub/Sub messageId={}; acknowledging to avoid infinite retry", messageId, ex);
            message.ack();
        }
    }

    private String resolveEventType(Map<String, String> attributes) {
        if (attributes == null || !attributes.containsKey(ATTR_EVENT_TYPE)) {
            return EVENT_USER_CREATED;
        }
        String eventType = attributes.get(ATTR_EVENT_TYPE);
        return (eventType == null || eventType.isBlank()) ? EVENT_USER_CREATED : eventType.trim();
    }
}

