package com.saumya.async_notification_service.dto;

public record BulkImportResponse(
        long totalRecords,
        long imported,
        long duplicates,
        long invalid
) {
}