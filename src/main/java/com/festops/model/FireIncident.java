package com.festops.model;

/**
 * A fire incident — critical with the tightest SLA, handled by the fire crew.
 */
public class FireIncident extends Incident {

    public FireIncident(String id, String description, double lat, double lng) {
        super(id, description, lat, lng);
    }

    @Override
    public IncidentType getType() {
        return IncidentType.FIRE;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }

    @Override
    public String requiredSkill() {
        return "fire";
    }

    @Override
    public long slaSeconds() {
        return 90;
    }
}
