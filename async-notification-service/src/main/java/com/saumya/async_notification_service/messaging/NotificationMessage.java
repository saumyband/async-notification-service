package com.saumya.async_notification_service.messaging;

public record NotificationMessage(
        String notificationId,
        String email,
        String subject,
        String message
) {
}