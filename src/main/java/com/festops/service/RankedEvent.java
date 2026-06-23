package com.festops.service;

import com.festops.model.Event;

/**
 * An {@link Event} scored for proximity/time/tag relevance by
 * {@link ConciergeService#getNearbyEvents}.
 */
public record RankedEvent(
        Event event,
        double distanceMeters,
        long minutesUntilStart,
        double score) {
}
