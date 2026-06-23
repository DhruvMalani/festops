package com.festops.controller.dto;

import com.festops.model.Responder;

/**
 * Outbound JSON view of a {@link Responder}, including availability.
 */
public record ResponderView(
        String id,
        String name,
        String skill,
        double lat,
        double lng,
        boolean available,
        int currentLoad) {

    public static ResponderView from(Responder r) {
        return new ResponderView(
                r.getId(), r.getName(), r.getSkill(),
                r.getLat(), r.getLng(), r.isAvailable(), r.getCurrentLoad());
    }
}
