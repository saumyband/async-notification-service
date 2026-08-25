package com.saumya.async_notification_service.messaging;

import com.saumya.async_notification_service.config.RabbitMQConfig;
import com.saumya.async_notification_service.model.DeliveryStatus;
import com.saumya.async_notification_service.model.NotificationDelivery;
import com.saumya.async_notification_service.repository.NotificationDeliveryRepository;
import com.saumya.async_notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final EmailService emailService;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consume(NotificationMessage message) {

        log.info(
                "Processing notificationId={} recipient={}",
                message.notificationId(),
                message.email()
        );

        emailService.sendEmail(
                message.email(),
                message.subject(),
                message.message()
        );

        NotificationDelivery delivery =
                notificationDeliveryRepository
                        .findByNotificationIdAndSubscriberEmail(
                                message.notificationId(),
                                message.email()
                        )
                        .orElseThrow();

        delivery.setStatus(DeliveryStatus.SENT);
        delivery.setSentAt(LocalDateTime.now());

        notificationDeliveryRepository.save(delivery);
    }
}