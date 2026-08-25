package com.saumya.async_notification_service.messaging;

import com.saumya.async_notification_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(NotificationMessage message) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_QUEUE,
                message
        );
    }
}