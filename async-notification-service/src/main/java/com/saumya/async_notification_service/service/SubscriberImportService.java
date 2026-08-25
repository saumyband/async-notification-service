package com.saumya.async_notification_service.service;

import com.saumya.async_notification_service.dto.BulkImportResponse;
import com.saumya.async_notification_service.model.Subscriber;
import com.saumya.async_notification_service.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SubscriberImportService {

    private static final int BATCH_SIZE = 500;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
            );

    private final SubscriberRepository subscriberRepository;

    public BulkImportResponse importCsv(MultipartFile file) {
        validateFile(file);

        BulkImportStats stats = new BulkImportStats();

        /*
         * Detect duplicates within this uploaded file.
         */
        Set<String> processedEmails = new HashSet<>();

        /*
         * Only one batch is kept in memory at a time.
         */
        List<String> batch = new ArrayList<>(BATCH_SIZE);

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        file.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );

                CSVParser parser =
                        CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreHeaderCase(true)
                                .setTrim(true)
                                .get()
                                .parse(reader)
        ) {
            validateHeaders(parser);

            for (CSVRecord record : parser) {
                stats.incrementTotal();
                String email = normalizeEmail(
                        record.get("email")
                );

                if (!isValidEmail(email)) {
                    stats.incrementInvalid();
                    continue;
                }

                /*
                 * HashSet.add() returns false if email
                 * was already encountered.
                 */
                if (!processedEmails.add(email)) {
                    stats.incrementDuplicates(1);
                    continue;
                }
                batch.add(email);

                if (batch.size() >= BATCH_SIZE) {
                    processBatch(batch, stats);
                    batch.clear();
                }
            }

            /*
             * Process final partial batch.
             */
            if (!batch.isEmpty()) {
                processBatch(batch, stats);
            }

            return stats.toResponse();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to process subscriber CSV",
                    e
            );
        }
    }

    private void processBatch(
            List<String> emails,
            BulkImportStats stats) {

        List<Subscriber> existingSubscribers =
                subscriberRepository.findByEmailIn(emails);

        Set<String> existingEmails = new HashSet<>();

        for (Subscriber subscriber : existingSubscribers) {
            existingEmails.add(
                    subscriber.getEmail().toLowerCase()
            );
        }

        List<Subscriber> newSubscribers = new ArrayList<>();

        for (String email : emails) {
            if (existingEmails.contains(email)) {
                stats.incrementDuplicates(1);
                continue;
            }

            Subscriber subscriber =
                    Subscriber.builder()
                            .email(email)
                            .active(true)
                            .subscribedAt(LocalDateTime.now())
                            .build();
            newSubscribers.add(subscriber);
        }

        if (!newSubscribers.isEmpty()) {
            subscriberRepository.saveAll(
                    newSubscribers
            );

            stats.incrementImported(
                    newSubscribers.size()
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV file cannot be empty"
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(".csv")) {

            throw new IllegalArgumentException(
                    "Only CSV files are supported"
            );
        }
    }

    private void validateHeaders(CSVParser parser) {

        if (!parser.getHeaderMap().containsKey("email")) {
            throw new IllegalArgumentException(
                    "CSV must contain an 'email' column"
            );
        }
    }

    private String normalizeEmail(String email) {

        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
    }

    private boolean isValidEmail(String email) {

        return email != null
                && !email.isBlank()
                && EMAIL_PATTERN
                .matcher(email)
                .matches();
    }
}