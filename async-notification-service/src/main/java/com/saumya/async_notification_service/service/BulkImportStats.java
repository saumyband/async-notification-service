package com.saumya.async_notification_service.service;

import com.saumya.async_notification_service.dto.BulkImportResponse;

public class BulkImportStats {

    private long total;
    private long imported;
    private long duplicates;
    private long invalid;

    public void incrementTotal() {
        total++;
    }

    public void incrementImported(long count) {
        imported += count;
    }

    public void incrementDuplicates(long count) {
        duplicates += count;
    }

    public void incrementInvalid() {
        invalid++;
    }

    public BulkImportResponse toResponse() {
        return new BulkImportResponse(
                total,
                imported,
                duplicates,
                invalid
        );
    }
}