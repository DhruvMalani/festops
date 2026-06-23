package com.festops.model;

import java.time.LocalDateTime;

/**
 * A location ping reported by a festival attendee.
 */
public record UserLocation(
        String userId,
        double latitude,
        double longitude,
        LocalDateTime timestamp) {
}
