package com.osvaldevops.notifications.infrastructure.adapter.in.web.mapper;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.NotificationResponse;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.SendNotificationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between inbound web DTOs and domain notification models.
 */
@Mapper(componentModel = "spring")
public interface NotificationWebMapper {

    /**
     * Maps a {@link SendNotificationRequest} DTO to a domain {@link UserCreatedEvent}.
     */
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    UserCreatedEvent toEvent(SendNotificationRequest request);

    /**
     * Maps a domain {@link WelcomeNotification} to a {@link NotificationResponse} DTO.
     */
    NotificationResponse toResponse(WelcomeNotification notification);
}

