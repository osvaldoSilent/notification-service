package com.osvaldevops.notifications.domain.port.in;

import com.osvaldevops.notifications.domain.event.UserCreatedEvent;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;

/**
 * Inbound port (use case) for composing and dispatching a welcome notification to a newly created user.
 */
public interface SendWelcomeNotificationUseCase {

    /**
     * Composes and sends a welcome notification triggered by a {@link UserCreatedEvent}.
     *
     * @param event user creation event containing recipient metadata
     * @return the dispatched {@link WelcomeNotification} domain model
     */
    WelcomeNotification sendWelcomeNotification(UserCreatedEvent event);
}

