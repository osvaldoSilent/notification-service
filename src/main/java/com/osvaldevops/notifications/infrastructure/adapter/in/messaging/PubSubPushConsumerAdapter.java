package com.osvaldevops.notifications.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.exception.InvalidPubSubMessageException;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.PubSubPushRequest;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.PubSubPushRequest.PubSubMessage;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper.UserCreatedEventMapper;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound Google Cloud Pub/Sub Push HTTP adapter designed for Google Cloud Run.
 * Receives push webhook envelopes from GCP Pub/Sub, decodes the Base64 JSON payload,
 * routes by {@code eventType} attribute, and invokes the corresponding domain use case.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pubsub")
@RequiredArgsConstructor
@Tag(name = "GCP Pub/Sub Push Webhook", description = "Cloud Run push endpoint consuming events from Google Cloud Pub/Sub")
public class PubSubPushConsumerAdapter {

    private static final String ATTR_EVENT_TYPE = "eventType";
    private static final String EVENT_USER_CREATED = "USER_CREATED";

    private final SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;
    private final UserCreatedEventMapper userCreatedEventMapper;
    private final ObjectMapper objectMapper;

    /**
     * Receives a Google Cloud Pub/Sub Push notification, decodes its Base64 payload,
     * routes the event by {@code message.attributes.eventType}, and triggers email delivery.
     * Returning {@code 204 No Content} acknowledges (ACKs) the message in GCP Pub/Sub.
     *
     * @param pushRequest standard GCP Pub/Sub push envelope
     * @return {@code 204 No Content} to acknowledge successful processing
     */
    @Operation(
            summary = "Consume GCP Pub/Sub Push event",
            description = "Processes push messages sent by a Google Cloud Pub/Sub Push Subscription on Cloud Run, routing by message.attributes.eventType."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event processed and acknowledged (ACK)"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid Pub/Sub envelope, Base64 data, JSON payload, or unsupported eventType",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Notification delivery failed (triggers Pub/Sub NACK and retry)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> onPubSubPushMessage(@RequestBody PubSubPushRequest pushRequest) {
        if (pushRequest == null || pushRequest.message() == null) {
            throw new InvalidPubSubMessageException("Pub/Sub push request and message envelope must not be null");
        }

        PubSubMessage message = pushRequest.message();
        if (message.data() == null || message.data().isBlank()) {
            throw new InvalidPubSubMessageException("Pub/Sub message data must not be null or blank");
        }

        String decodedJson = decodeBase64Payload(message.data());
        String eventType = resolveEventType(message.attributes());

        log.info("Received GCP Pub/Sub Push messageId={} subscription={} eventType={}",
                message.messageId(), pushRequest.subscription(), eventType);

        if (EVENT_USER_CREATED.equalsIgnoreCase(eventType)) {
            handleUserCreatedEvent(decodedJson);
        } else {
            throw new InvalidPubSubMessageException("Unsupported Pub/Sub eventType: " + eventType);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves the event type from Pub/Sub message attributes, defaulting to {@code USER_CREATED}
     * when no attribute is provided.
     */
    private String resolveEventType(Map<String, String> attributes) {
        if (attributes == null || !attributes.containsKey(ATTR_EVENT_TYPE)) {
            return EVENT_USER_CREATED;
        }
        String eventType = attributes.get(ATTR_EVENT_TYPE);
        return (eventType == null || eventType.isBlank()) ? EVENT_USER_CREATED : eventType.trim();
    }

    /**
     * Decodes a Base64-encoded Pub/Sub message data string into a UTF-8 JSON string.
     */
    private String decodeBase64Payload(String base64Data) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data.trim());
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new InvalidPubSubMessageException("Failed to decode Base64 Pub/Sub message data", ex);
        }
    }

    /**
     * Deserializes a {@link UserCreatedMessagePayload} from JSON and invokes the welcome notification use case.
     */
    private void handleUserCreatedEvent(String decodedJson) {
        try {
            UserCreatedMessagePayload payload = objectMapper.readValue(decodedJson, UserCreatedMessagePayload.class);
            UserCreatedEvent event = userCreatedEventMapper.toDomainEvent(payload);
            sendWelcomeNotificationUseCase.sendWelcomeNotification(event);
        } catch (JsonProcessingException ex) {
            throw new InvalidPubSubMessageException("Malformed JSON payload for USER_CREATED event", ex);
        }
    }
}

