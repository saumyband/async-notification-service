package com.saumya.async_notification_service.controller;

import com.saumya.async_notification_service.dto.NotificationRequest;
import com.saumya.async_notification_service.dto.NotificationStatusResponse;
import com.saumya.async_notification_service.model.Notification;
import com.saumya.async_notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Notification sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        return notificationService.sendNotification(request);
    }

    @GetMapping("/{notificationId}/status")
    public NotificationStatusResponse getStatus(
            @PathVariable String notificationId) {

        return notificationService.getStatus(notificationId);
    }
}