package com.saumya.async_notification_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "subscribers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscriber {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private boolean active;

    private LocalDateTime subscribedAt;
}