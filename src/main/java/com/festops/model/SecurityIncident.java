package com.festops.model;

/**
 * A security incident — handled by security staff.
 */
public class SecurityIncident extends Incident {

    public SecurityIncident(String id, String description, double lat, double lng) {
        super(id, description, lat, lng);
    }

    @Override
    public IncidentType getType() {
        return IncidentType.SECURITY;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.HIGH;
    }

    @Override
    public String requiredSkill() {
        return "security";
    }

    @Override
    public long slaSeconds() {
        return 180;
    }
}
