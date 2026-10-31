package com.osvaldevops.notifications.infrastructure.adapter.out.notification;

import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.domain.port.out.NotificationSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter implementing {@link NotificationSenderPort} to simulate/dispatch
 * welcome email notifications and log delivery details.
 */
@Slf4j
@Component
public class EmailNotificationSenderAdapter implements NotificationSenderPort {

    private final String senderAddress;

    public EmailNotificationSenderAdapter(
            @Value("${app.notifications.sender-address:no-reply@osvaldevops.com}") String senderAddress
    ) {
        this.senderAddress = senderAddress;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(WelcomeNotification notification) {
        if (notification == null || notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            throw new NotificationDeliveryException("Cannot send welcome notification without a valid recipient email");
        }

        // Log and simulate email dispatch to the newly registered user
        log.info("Dispatching welcome email from={} to={} subject=\"{}\" notificationId={} userId={}",
                senderAddress,
                notification.getRecipientEmail(),
                notification.getSubject(),
                notification.getId(),
                notification.getUserId());
        log.debug("Welcome email body: {}", notification.getBody());
    }
}

