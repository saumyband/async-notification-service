package com.saumya.async_notification_service.service;

import com.saumya.async_notification_service.dto.SubscriberRequest;
import com.saumya.async_notification_service.model.Subscriber;
import com.saumya.async_notification_service.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;

    public Subscriber subscribe(SubscriberRequest request) {

        Subscriber existingSubscriber =
                subscriberRepository.findByEmail(request.getEmail())
                        .orElse(null);

        if (existingSubscriber != null) {
            existingSubscriber.setActive(true);
            existingSubscriber.setSubscribedAt(LocalDateTime.now());

            return subscriberRepository.save(existingSubscriber);
        }

        Subscriber subscriber = Subscriber.builder()
                .email(request.getEmail())
                .active(true)
                .subscribedAt(LocalDateTime.now())
                .build();

        return subscriberRepository.save(subscriber);
    }

    public void unsubscribe(String email) {

        Subscriber subscriber = subscriberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Subscriber not found"));

        subscriber.setActive(false);

        subscriberRepository.save(subscriber);
    }
}