package com.saumya.async_notification_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notification_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDelivery {

    @Id
    private String id;

    private String notificationId;

    private String subscriberEmail;

    private DeliveryStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private String errorMessage;
}