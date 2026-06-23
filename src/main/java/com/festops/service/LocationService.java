package com.festops.service;

import com.festops.model.UserLocation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores the latest reported location per user in a {@link ConcurrentHashMap}.
 */
@Service
public class LocationService {

    private final Map<String, UserLocation> locations = new ConcurrentHashMap<>();

    /** Record a ping (timestamped server-side) and return the stored location. */
    public UserLocation record(String userId, double latitude, double longitude) {
        UserLocation location = new UserLocation(userId, latitude, longitude, LocalDateTime.now());
        locations.put(userId, location);
        return location;
    }

    public UserLocation getLatest(String userId) {
        return locations.get(userId);
    }

    public Map<String, UserLocation> getAll() {
        return locations;
    }
}
