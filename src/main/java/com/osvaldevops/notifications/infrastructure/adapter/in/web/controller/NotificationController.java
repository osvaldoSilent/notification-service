package com.osvaldevops.notifications.infrastructure.adapter.in.web.controller;

import com.osvaldevops.notifications.domain.model.WelcomeNotification;
import com.osvaldevops.notifications.domain.port.in.SendWelcomeNotificationUseCase;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.ApiErrorResponse;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.NotificationResponse;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.dto.SendNotificationRequest;
import com.osvaldevops.notifications.infrastructure.adapter.in.web.mapper.NotificationWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound REST controller exposing endpoints to trigger welcome notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for triggering and testing user notifications")
public class NotificationController {

    private final SendWelcomeNotificationUseCase sendWelcomeNotificationUseCase;
    private final NotificationWebMapper notificationWebMapper;

    /**
     * Composes and dispatches a welcome notification for a user.
     *
     * @param request validated notification request payload
     * @return {@code 201 Created} with {@link NotificationResponse} details
     */
    @Operation(
            summary = "Send a welcome notification",
            description = "Composes and dispatches a welcome notification to the specified user email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Welcome notification successfully sent",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or malformed JSON",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Notification delivery service unavailable",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping("/welcome")
    public ResponseEntity<NotificationResponse> sendWelcome(@Valid @RequestBody SendNotificationRequest request) {
        WelcomeNotification notification = sendWelcomeNotificationUseCase.sendWelcomeNotification(
                notificationWebMapper.toEvent(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationWebMapper.toResponse(notification));
    }
}

