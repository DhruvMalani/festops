package com.festops.controller;

import com.festops.controller.dto.LocationPing;
import com.festops.model.UserLocation;
import com.festops.service.ConciergeService;
import com.festops.service.LocationService;
import com.festops.service.RankedEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Location intake and the nearby-events concierge endpoint.
 */
@RestController
public class LocationController {

    private final LocationService locationService;
    private final ConciergeService conciergeService;

    public LocationController(LocationService locationService, ConciergeService conciergeService) {
        this.locationService = locationService;
        this.conciergeService = conciergeService;
    }

    /** Store a user location ping. */
    @PostMapping("/api/v1/location")
    public ResponseEntity<UserLocation> ping(@RequestBody LocationPing request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("Missing 'userId'");
        }
        UserLocation stored = locationService.record(
                request.userId(), request.latitude(), request.longitude());
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    /**
     * Ranked nearby events. {@code tags} is an optional comma-separated list,
     * e.g. {@code ?lat=28.3635&lng=75.5870&tags=tech,coding}.
     */
    @GetMapping("/api/v1/events/nearby")
    public List<RankedEvent> nearby(@RequestParam double lat,
                                    @RequestParam double lng,
                                    @RequestParam(required = false) String tags) {
        List<String> tagList = (tags == null || tags.isBlank())
                ? List.of()
                : Arrays.stream(tags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
        return conciergeService.getNearbyEvents(lat, lng, tagList);
    }
}
