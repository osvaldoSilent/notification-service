package com.osvaldevops.notifications.infrastructure.adapter.out.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.osvaldevops.notifications.domain.exception.NotificationDeliveryException;
import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit tests for {@link EmailNotificationSenderAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderAdapterTest {

    private static final String SENDER_ADDRESS = "onboarding@resend.dev";

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    @DisplayName("Should log welcome notification without calling JavaMailSender when mailEnabled is false")
    void should_SimulateNotificationOnly_When_MailEnabledIsFalse() {
        // Given
        EmailNotificationSenderAdapter adapter = new EmailNotificationSenderAdapter(
                javaMailSender,
                SENDER_ADDRESS,
                false
        );
        WelcomeNotification notification = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .recipientEmail("osval@osvaldevops.com")
                .recipientName("osval")
                .subject("Welcome!")
                .body("Hello osval!")
                .status("SENT")
                .sentAt(Instant.now())
                .build();

        // When / Then
        assertThatCode(() -> adapter.send(notification)).doesNotThrowAnyException();
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send SimpleMailMessage via JavaMailSender when mailEnabled is true")
    void should_SendEmailViaJavaMailSender_When_MailEnabledIsTrue() {
        // Given
        EmailNotificationSenderAdapter adapter = new EmailNotificationSenderAdapter(
                javaMailSender,
                SENDER_ADDRESS,
                true
        );
        WelcomeNotification notification = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .recipientEmail("osval@osvaldevops.com")
                .recipientName("osval")
                .subject("Welcome to OsvalDevOps!")
                .body("Hello osval, welcome aboard!")
                .status("SENT")
                .sentAt(Instant.now())
                .build();

        // When
        adapter.send(notification);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage sentMail = captor.getValue();
        assertThat(sentMail.getFrom()).isEqualTo(SENDER_ADDRESS);
        assertThat(sentMail.getTo()).containsExactly("osval@osvaldevops.com");
        assertThat(sentMail.getSubject()).isEqualTo("Welcome to OsvalDevOps!");
        assertThat(sentMail.getText()).isEqualTo("Hello osval, welcome aboard!");
    }

    @Test
    @DisplayName("Should wrap MailException into NotificationDeliveryException when SMTP sending fails")
    void should_ThrowNotificationDeliveryException_When_JavaMailSenderThrowsMailException() {
        // Given
        EmailNotificationSenderAdapter adapter = new EmailNotificationSenderAdapter(
                javaMailSender,
                SENDER_ADDRESS,
                true
        );
        WelcomeNotification notification = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .recipientEmail("osval@osvaldevops.com")
                .recipientName("osval")
                .subject("Welcome!")
                .body("Hello osval!")
                .status("SENT")
                .sentAt(Instant.now())
                .build();

        doThrow(new MailSendException("SMTP connection refused"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        // When / Then
        assertThatThrownBy(() -> adapter.send(notification))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Failed to send welcome email via SMTP")
                .hasCauseInstanceOf(MailSendException.class);
    }

    @Test
    @DisplayName("Should throw NotificationDeliveryException when notification or recipient email is null or blank")
    void should_ThrowNotificationDeliveryException_When_NotificationOrRecipientEmailIsInvalid() {
        // Given
        EmailNotificationSenderAdapter adapter = new EmailNotificationSenderAdapter(
                javaMailSender,
                SENDER_ADDRESS,
                false
        );
        WelcomeNotification nullEmail = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .recipientEmail(null)
                .build();
        WelcomeNotification blankEmail = WelcomeNotification.builder()
                .id(UUID.randomUUID())
                .recipientEmail("   ")
                .build();

        // When / Then
        assertThatThrownBy(() -> adapter.send(null))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Cannot send welcome notification without a valid recipient email");

        assertThatThrownBy(() -> adapter.send(nullEmail))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Cannot send welcome notification without a valid recipient email");

        assertThatThrownBy(() -> adapter.send(blankEmail))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessage("Cannot send welcome notification without a valid recipient email");
    }
}
