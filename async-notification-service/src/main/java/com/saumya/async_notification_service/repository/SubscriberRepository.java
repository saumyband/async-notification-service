package com.saumya.async_notification_service.repository;

import com.saumya.async_notification_service.model.Subscriber;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriberRepository
        extends MongoRepository<Subscriber, String> {

    Optional<Subscriber> findByEmail(String email);

    List<Subscriber> findByActiveTrue();

    List<Subscriber> findByEmailIn(Collection<String> emails);

}