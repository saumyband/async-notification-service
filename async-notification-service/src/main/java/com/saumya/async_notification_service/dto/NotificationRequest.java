package com.saumya.async_notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}