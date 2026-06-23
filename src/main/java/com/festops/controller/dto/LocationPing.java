package com.festops.controller.dto;

/**
 * Inbound JSON body for {@code POST /api/v1/location}.
 */
public record LocationPing(String userId, double latitude, double longitude) {
}
