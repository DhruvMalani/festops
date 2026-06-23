package com.festops.controller;

import com.festops.controller.dto.NearbyResponderView;
import com.festops.controller.dto.ResponderStatusUpdate;
import com.festops.controller.dto.ResponderView;
import com.festops.service.FestOpsService;
import com.festops.util.HaversineUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read and availability-update endpoints for responders.
 */
@RestController
@RequestMapping("/api/v1/responders")
public class ResponderController {

    private final FestOpsService service;

    public ResponderController(FestOpsService service) {
        this.service = service;
    }

    /** List all responders with their availability. */
    @GetMapping
    public List<ResponderView> list() {
        return service.listResponders().stream()
                .map(ResponderView::from)
                .toList();
    }

    /**
     * Responders ranked nearest-first from {@code lat}/{@code lng}, optionally
     * filtered by {@code skill}.
     */
    @GetMapping("/nearby")
    public List<NearbyResponderView> nearby(@RequestParam double lat,
                                            @RequestParam double lng,
                                            @RequestParam(required = false) String skill) {
        return service.findNearby(lat, lng, skill).stream()
                .map(r -> NearbyResponderView.from(
                        r, HaversineUtil.distanceKm(lat, lng, r.getLat(), r.getLng())))
                .toList();
    }

    /** Update a responder's availability. */
    @PatchMapping("/{id}/status")
    public ResponderView updateStatus(@PathVariable String id,
                                      @RequestBody ResponderStatusUpdate request) {
        if (request.available() == null) {
            throw new IllegalArgumentException("Missing 'available' (expected true or false)");
        }
        return ResponderView.from(
                service.updateResponderAvailability(id, request.available()));
    }
}
