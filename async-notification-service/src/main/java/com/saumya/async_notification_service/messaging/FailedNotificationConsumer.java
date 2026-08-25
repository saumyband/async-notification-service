package com.saumya.async_notification_service.messaging;

import com.saumya.async_notification_service.config.RabbitMQConfig;
import com.saumya.async_notification_service.model.DeliveryStatus;
import com.saumya.async_notification_service.model.NotificationDelivery;
import com.saumya.async_notification_service.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedNotificationConsumer {

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void consumeFailed(NotificationMessage message) {

        NotificationDelivery delivery =
                notificationDeliveryRepository
                        .findByNotificationIdAndSubscriberEmail(
                                message.notificationId(),
                                message.email()
                        )
                        .orElseThrow();

        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setErrorMessage(
                "Email delivery failed after retries"
        );

        notificationDeliveryRepository.save(delivery);

        log.error(
                "Notification delivery permanently failed notificationId={} recipient={}",
                message.notificationId(),
                message.email()
        );
    }
}