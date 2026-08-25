package com.saumya.async_notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscriberRequest {

    @NotBlank
    @Email
    private String email;
}