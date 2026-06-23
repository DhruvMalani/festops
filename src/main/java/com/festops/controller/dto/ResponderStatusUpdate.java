package com.festops.controller.dto;

/**
 * Inbound JSON body for {@code PATCH /api/v1/responders/{id}/status}.
 */
public record ResponderStatusUpdate(Boolean available) {
}
