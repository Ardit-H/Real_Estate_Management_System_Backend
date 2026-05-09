package com.realestate.backend.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.realestate.backend.entity.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.io.Serializable;

public class NotificationDtos {

    // ── CREATE (nga sistemi / service) ────────────────────────

    public record NotificationCreateRequest(

            @NotNull(message = "user_id është i detyrueshëm")
            @JsonProperty("user_id")
            Long userId,

            @NotBlank
            @Size(max = 255)
            String title,

            @NotBlank
            String message,

            @Schema(allowableValues = {"INFO","WARNING","SUCCESS","ERROR","REMINDER"})
            NotificationType type,

            @JsonProperty("related_entity_type") String relatedEntityType,
            @JsonProperty("related_entity_id")   Long relatedEntityId,
            @JsonProperty("action_url")          String actionUrl
    ) {}

    // ── RESPONSE ──────────────────────────────────────────────

    public record NotificationResponse(
            Long id,
            @JsonProperty("user_id")              Long userId,
            String title,
            String message,
            NotificationType type,
            @JsonProperty("related_entity_type")  String relatedEntityType,
            @JsonProperty("related_entity_id")    Long relatedEntityId,
            @JsonProperty("action_url")           String actionUrl,
            @JsonProperty("is_read")              Boolean isRead,
            @JsonProperty("read_at")              LocalDateTime readAt,
            @JsonProperty("created_at")           LocalDateTime createdAt
    ) {}

    // ── UNREAD COUNT ──────────────────────────────────────────

    public record UnreadCountResponse(
            @JsonProperty("unread_count") long unreadCount
    )implements Serializable {}

    // ── BATCH READ ────────────────────────────────────────────

    public record BatchReadResponse(
            int marked,
            String message
    ) {}

    // ── PAGINATED WRAPPER ─────────────────────────────────────

    public record NotificationPageResponse(
            List<NotificationResponse> notifications,
            @JsonProperty("unread_count") long unreadCount,
            @JsonProperty("total_elements") long totalElements
    ) {}
}

