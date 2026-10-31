package com.osvaldevops.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test verifying that the application entry point class is present.
 */
class NotificationServiceApplicationTests {

    @Test
    @DisplayName("Should load NotificationServiceApplication class definition")
    void should_VerifyApplicationClassExists_When_Loaded() {
        // Given / When / Then
        assertThat(NotificationServiceApplication.class).isNotNull();
    }
}
