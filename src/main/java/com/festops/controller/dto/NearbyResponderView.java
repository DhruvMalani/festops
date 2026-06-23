package com.festops.controller.dto;

import com.festops.model.Responder;

/**
 * Outbound JSON view of a {@link Responder} ranked by proximity, with the
 * great-circle distance (km) from the query point.
 */
public record NearbyResponderView(
        String id,
        String name,
        String skill,
        double lat,
        double lng,
        boolean available,
        int currentLoad,
        double distanceKm) {

    public static NearbyResponderView from(Responder r, double distanceKm) {
        return new NearbyResponderView(
                r.getId(), r.getName(), r.getSkill(),
                r.getLat(), r.getLng(), r.isAvailable(), r.getCurrentLoad(),
                Math.round(distanceKm * 1000.0) / 1000.0);
    }
}
