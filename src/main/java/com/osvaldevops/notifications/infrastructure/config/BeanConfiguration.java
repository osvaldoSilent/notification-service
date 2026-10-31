package com.osvaldevops.notifications.infrastructure.config;

import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.domain.port.out.NotificationSenderPort;
import com.osvaldevops.notifications.domain.service.WelcomeNotificationDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring pure domain services with outbound infrastructure ports.
 */
@Configuration
public class BeanConfiguration {

    /**
     * Registers {@link WelcomeNotificationDomainService} as the implementation of
     * {@link SendWelcomeNotificationUseCase} while keeping the domain package framework-free.
     */
    @Bean
    public SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase(
            NotificationSenderPort notificationSenderPort
    ) {
        return new WelcomeNotificationDomainService(notificationSenderPort);
    }
}

