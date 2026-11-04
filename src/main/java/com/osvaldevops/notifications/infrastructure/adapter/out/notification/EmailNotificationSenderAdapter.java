package com.osvaldevops.notifications.infrastructure.adapter.out.notification;

import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.domain.port.out.NotificationSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter implementing {@link NotificationSenderPort} to dispatch
 * welcome email notifications via SMTP ({@link JavaMailSender}) or log them in simulation mode.
 */
@Slf4j
@Component
public class EmailNotificationSenderAdapter implements NotificationSenderPort {

    private final JavaMailSender javaMailSender;
    private final String senderAddress;
    private final boolean mailEnabled;

    public EmailNotificationSenderAdapter(
            JavaMailSender javaMailSender,
            @Value("${app.notifications.sender-address}") String senderAddress,
            @Value("${app.notifications.mail-enabled}") boolean mailEnabled
    ) {
        this.javaMailSender = javaMailSender;
        this.senderAddress = senderAddress;
        this.mailEnabled = mailEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(WelcomeNotification notification) {
        if (notification == null || notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            throw new NotificationDeliveryException("Cannot send welcome notification without a valid recipient email");
        }

        if (!mailEnabled) {
            log.info("[SIMULATION] Dispatching welcome email from={} to={} subject=\"{}\" notificationId={} userId={}",
                    senderAddress,
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getId(),
                    notification.getUserId());
            log.debug("[SIMULATION] Welcome email body: {}", notification.getBody());
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(senderAddress);
            mailMessage.setTo(notification.getRecipientEmail());
            mailMessage.setSubject(notification.getSubject());
            mailMessage.setText(notification.getBody());

            javaMailSender.send(mailMessage);

            log.info("Sent welcome email via SMTP from={} to={} subject=\"{}\" notificationId={} userId={}",
                    senderAddress,
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getId(),
                    notification.getUserId());
        } catch (MailException ex) {
            throw new NotificationDeliveryException("Failed to send welcome email via SMTP", ex);
        }
    }
}
