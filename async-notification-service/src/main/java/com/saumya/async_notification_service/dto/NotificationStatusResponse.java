package com.saumya.async_notification_service.dto;

public record NotificationStatusResponse(
        String notificationId,
        long total,
        long pending,
        long sent,
        long failed
) {
}