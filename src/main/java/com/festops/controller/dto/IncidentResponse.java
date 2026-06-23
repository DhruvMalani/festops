package com.festops.controller.dto;

import com.festops.model.Incident;
import com.festops.model.Responder;

import java.util.List;

/**
 * Outbound JSON view of an {@link Incident}. Built explicitly (rather than
 * serializing the entity) so internal lifecycle/observer state is not exposed.
 * {@code auditTrail} is populated only on the single-incident detail endpoint.
 */
public record IncidentResponse(
        String id,
        String type,
        String severity,
        int severityLevel,
        String requiredSkill,
        long slaSeconds,
        String description,
        double latitude,
        double longitude,
        String state,
        String reportedAt,
        ResponderSummary assignedResponder,
        List<String> auditTrail) {

    /** Nested summary of the assigned responder. */
    public record ResponderSummary(String id, String name, String skill) {
        static ResponderSummary from(Responder r) {
            return r == null ? null : new ResponderSummary(r.getId(), r.getName(), r.getSkill());
        }
    }

    public static IncidentResponse from(Incident incident) {
        return build(incident, null);
    }

    public static IncidentResponse from(Incident incident, List<String> auditTrail) {
        return build(incident, auditTrail);
    }

    private static IncidentResponse build(Incident incident, List<String> auditTrail) {
        return new IncidentResponse(
                incident.getId(),
                incident.getType().name(),
                incident.getSeverity().name(),
                incident.getSeverity().getLevel(),
                incident.requiredSkill(),
                incident.slaSeconds(),
                incident.getDescription(),
                incident.getLat(),
                incident.getLng(),
                incident.getStateName(),
                incident.getReportedAt().toString(),
                ResponderSummary.from(incident.getAssignedResponder()),
                auditTrail);
    }
}
