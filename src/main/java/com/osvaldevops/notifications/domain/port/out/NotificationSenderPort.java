package com.osvaldevops.notifications.domain.port.out;

import com.osvaldevops.notifications.domain.model.WelcomeNotification;

/**
 * Outbound port for dispatching notifications through an external delivery channel (e.g., Email / Log).
 */
public interface NotificationSenderPort {

    /**
     * Dispatches the given {@link WelcomeNotification} to its recipient.
     *
     * @param notification welcome notification to deliver
     */
    void send(WelcomeNotification notification);
}

