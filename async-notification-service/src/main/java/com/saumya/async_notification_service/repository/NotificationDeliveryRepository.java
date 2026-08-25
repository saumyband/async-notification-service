package com.saumya.async_notification_service.repository;

import com.saumya.async_notification_service.model.NotificationDelivery;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository
        extends MongoRepository<NotificationDelivery, String> {

    List<NotificationDelivery> findByNotificationId(String notificationId);

    Optional<NotificationDelivery>
    findByNotificationIdAndSubscriberEmail(
            String notificationId,
            String subscriberEmail
    );
}