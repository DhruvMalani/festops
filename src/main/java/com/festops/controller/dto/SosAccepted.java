package com.festops.controller.dto;

/**
 * Response body for {@code POST /api/v1/sos}: the report has been accepted onto
 * the intake queue and will be processed asynchronously.
 */
public record SosAccepted(String incidentId, String status, String message) {
}
