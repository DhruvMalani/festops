package com.festops.controller.dto;

/**
 * Inbound JSON body for {@code PATCH /api/v1/incidents/{id}/status}.
 * {@code action} is one of: acknowledge, respond, resolve, escalate.
 */
public record StatusUpdateRequest(String action) {
}
