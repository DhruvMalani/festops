package com.festops.controller;

import com.festops.controller.dto.SosAccepted;
import com.festops.controller.dto.SosRequest;
import com.festops.service.FestOpsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Intake endpoint for SOS reports (producer side of the pipeline).
 */
@RestController
@RequestMapping("/api/v1/sos")
public class SosController {

    private final FestOpsService service;

    public SosController(FestOpsService service) {
        this.service = service;
    }

    /**
     * Accept an SOS report onto the intake queue and return immediately with
     * {@code 202 Accepted}. The incident is triaged, dispatched and stored
     * asynchronously by the consumer thread; poll
     * {@code GET /api/v1/incidents/{id}} for its status.
     */
    @PostMapping
    public ResponseEntity<SosAccepted> report(@RequestBody SosRequest request) {
        String incidentId = service.enqueue(
                request.reporterId(),
                request.description(),
                request.latitude(),
                request.longitude());

        SosAccepted body = new SosAccepted(
                incidentId, "QUEUED", "SOS accepted for processing");

        return ResponseEntity.accepted()
                .header(HttpHeaders.LOCATION, "/api/v1/incidents/" + incidentId)
                .body(body);
    }
}
