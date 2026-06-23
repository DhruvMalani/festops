package com.festops.controller.dto;

/**
 * Inbound JSON body for {@code POST /api/v1/sos}.
 */
public record SosRequest(
        String reporterId,
        String description,
        double latitude,
        double longitude) {
}
