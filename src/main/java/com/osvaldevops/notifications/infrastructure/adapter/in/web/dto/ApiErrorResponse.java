package com.osvaldevops.notifications.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * Standardized REST API error response DTO returned by {@code GlobalExceptionHandler}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response payload")
public record ApiErrorResponse(
        @Schema(description = "Timestamp when the error occurred", example = "2026-10-30T23:55:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,

        @Schema(description = "HTTP error reason phrase", example = "Bad Request", requiredMode = Schema.RequiredMode.REQUIRED)
        String error,

        @Schema(description = "Detailed error message", example = "Validation failed for one or more fields", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,

        @Schema(description = "Request URI path", example = "/api/v1/notifications/welcome", requiredMode = Schema.RequiredMode.REQUIRED)
        String path,

        @Schema(description = "Field-level validation errors (present only on 400 validation errors)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Map<String, String> validationErrors
) {}

