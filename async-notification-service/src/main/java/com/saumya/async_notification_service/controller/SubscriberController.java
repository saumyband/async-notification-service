package com.saumya.async_notification_service.controller;

import com.saumya.async_notification_service.dto.BulkImportResponse;
import com.saumya.async_notification_service.dto.SubscriberRequest;
import com.saumya.async_notification_service.model.Subscriber;
import com.saumya.async_notification_service.service.SubscriberImportService;
import com.saumya.async_notification_service.service.SubscriberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final SubscriberImportService subscriberImportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subscriber subscribe(
            @Valid @RequestBody SubscriberRequest request) {

        return subscriberService.subscribe(request);
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable String email) {
        subscriberService.unsubscribe(email);
    }

    @PostMapping(
            value = "/import",
            consumes = "multipart/form-data"
    )
    public BulkImportResponse importSubscribers(
            @RequestParam("file") MultipartFile file) {

        return subscriberImportService.importCsv(file);
    }
}