package com.festops.controller;

import com.festops.controller.dto.IncidentResponse;
import com.festops.controller.dto.StatusUpdateRequest;
import com.festops.model.Incident;
import com.festops.service.FestOpsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read and lifecycle-transition endpoints for incidents.
 */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final FestOpsService service;

    public IncidentController(FestOpsService service) {
        this.service = service;
    }

    /** List all incidents. */
    @GetMapping
    public List<IncidentResponse> list() {
        return service.listIncidents().stream()
                .map(IncidentResponse::from)
                .toList();
    }

    /** Get a single incident together with its audit trail. */
    @GetMapping("/{id}")
    public IncidentResponse getOne(@PathVariable String id) {
        Incident incident = service.getIncident(id);
        return IncidentResponse.from(incident, service.auditTrail(id));
    }

    /** Apply a lifecycle transition and return the updated incident. */
    @PatchMapping("/{id}/status")
    public IncidentResponse updateStatus(@PathVariable String id,
                                         @RequestBody StatusUpdateRequest request) {
        Incident incident = service.transition(id, request.action());
        return IncidentResponse.from(incident, service.auditTrail(id));
    }
}
