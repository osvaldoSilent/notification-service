package com.osvaldevops.notifications.infrastructure.adapter.in.messaging.mapper;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.infrastructure.adapter.in.messaging.dto.UserCreatedMessagePayload;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper converting inbound Kafka {@link UserCreatedMessagePayload} DTOs
 * into domain {@link UserCreatedEvent} objects.
 */
@Mapper(componentModel = "spring")
public interface UserCreatedEventMapper {

    /**
     * Maps a Kafka {@link UserCreatedMessagePayload} to a domain {@link UserCreatedEvent}.
     */
    UserCreatedEvent toDomainEvent(UserCreatedMessagePayload payload);
}

