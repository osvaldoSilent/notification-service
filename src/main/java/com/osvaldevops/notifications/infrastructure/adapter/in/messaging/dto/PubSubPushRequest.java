package com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Standard Google Cloud Pub/Sub Push HTTP request envelope delivered to Cloud Run.
 *
 * @param message      the Pub/Sub message containing Base64-encoded data and attributes
 * @param subscription the fully qualified GCP Pub/Sub subscription name
 */
@Schema(description = "Google Cloud Pub/Sub Push HTTP webhook envelope")
public record PubSubPushRequest(
        @Schema(description = "Wrapped Pub/Sub message object", requiredMode = Schema.RequiredMode.REQUIRED)
        PubSubMessage message,

        @Schema(
                description = "GCP Pub/Sub subscription identifier",
                example = "projects/osvaldevops-gcp/subscriptions/user-created-events-sub",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String subscription
) {

    /**
     * Inner Pub/Sub message representation inside the Push envelope.
     *
     * @param data        Base64-encoded JSON event payload
     * @param messageId   unique Pub/Sub message ID assigned by Google Cloud
     * @param publishTime ISO-8601 timestamp when the message was published
     * @param attributes  optional key-value metadata attributes (e.g., {@code eventType=USER_CREATED})
     */
    @Schema(description = "Pub/Sub message payload and metadata attributes")
    public record PubSubMessage(
            @Schema(
                    description = "Base64-encoded JSON payload of the domain event",
                    example = "eyJ1c2VySWQiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6Im9zdmFsQG9zdmFsZGV2b3BzLmNvbSIsInVzZXJOYW1lIjoib3N2YWwiLCJyb2xlIjoiUk9MRV9VU0VSIiwiY3JlYXRlZEF0IjoiMjAyNi0xMC0zMFQxOTowMDowMFoifQ==",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String data,

            @Schema(description = "Unique identifier of the Pub/Sub message", example = "1058293847192", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String messageId,

            @Schema(description = "Timestamp when the message was published to Pub/Sub", example = "2026-11-04T23:30:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String publishTime,

            @Schema(description = "Metadata attributes used for routing events (e.g., eventType=USER_CREATED)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Map<String, String> attributes
    ) {}
}

