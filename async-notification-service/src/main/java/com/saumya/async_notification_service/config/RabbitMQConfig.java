package com.saumya.async_notification_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE =
            "notification.queue";

    public static final String DLQ =
            "notification.dlq";

    public static final String DLX =
            "notification.dlx";

    public static final String DLQ_ROUTING_KEY =
            "notification.failed";

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue notificationQueue() {

        return QueueBuilder
                .durable(NOTIFICATION_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        DLX
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        DLQ_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DLQ)
                .build();
    }

    @Bean
    public Binding deadLetterBinding() {

        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}