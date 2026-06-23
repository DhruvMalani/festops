package com.festops.controller.dto;

/**
 * Inbound JSON body for {@code PATCH /api/v1/incidents/{id}/status}.
 * {@code status} is the target lifecycle state, one of:
 * ACKNOWLEDGED, RESPONDING, RESOLVED, ESCALATED.
 */
public record StatusUpdateRequest(String status) {
}
