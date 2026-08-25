package com.saumya.async_notification_service.service;

import com.saumya.async_notification_service.dto.NotificationRequest;
import com.saumya.async_notification_service.dto.NotificationStatusResponse;
import com.saumya.async_notification_service.messaging.NotificationMessage;
import com.saumya.async_notification_service.messaging.NotificationProducer;
import com.saumya.async_notification_service.model.DeliveryStatus;
import com.saumya.async_notification_service.model.Notification;
import com.saumya.async_notification_service.model.NotificationDelivery;
import com.saumya.async_notification_service.model.Subscriber;
import com.saumya.async_notification_service.repository.NotificationDeliveryRepository;
import com.saumya.async_notification_service.repository.NotificationRepository;
import com.saumya.async_notification_service.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SubscriberRepository subscriberRepository;
    private final EmailService emailService;
    private final NotificationProducer notificationProducer;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    public Notification sendNotification(NotificationRequest request) {

        Notification notification = Notification.builder()
                .subject(request.getSubject())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        List<Subscriber> subscribers =
                subscriberRepository.findByActiveTrue();

        for (Subscriber subscriber : subscribers) {

            NotificationDelivery delivery =
                    NotificationDelivery.builder()
                            .notificationId(notification.getId())
                            .subscriberEmail(subscriber.getEmail())
                            .status(DeliveryStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .build();

            notificationDeliveryRepository.save(delivery);

            NotificationMessage message =
                    new NotificationMessage(
                            notification.getId(),
                            subscriber.getEmail(),
                            notification.getSubject(),
                            notification.getMessage()
                    );

            notificationProducer.send(message);
        }

        return notification;
    }

    public NotificationStatusResponse getStatus(String notificationId) {

        List<NotificationDelivery> deliveries =
                notificationDeliveryRepository
                        .findByNotificationId(notificationId);

        long pending = deliveries.stream()
                .filter(d -> d.getStatus() == DeliveryStatus.PENDING)
                .count();

        long sent = deliveries.stream()
                .filter(d -> d.getStatus() == DeliveryStatus.SENT)
                .count();

        long failed = deliveries.stream()
                .filter(d -> d.getStatus() == DeliveryStatus.FAILED)
                .count();

        return new NotificationStatusResponse(
                notificationId,
                deliveries.size(),
                pending,
                sent,
                failed
        );
    }
}