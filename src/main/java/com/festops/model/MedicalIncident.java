package com.festops.model;

/**
 * A medical emergency — highest urgency, short SLA, handled by medics.
 */
public class MedicalIncident extends Incident {

    public MedicalIncident(String id, String description, double lat, double lng) {
        super(id, description, lat, lng);
    }

    @Override
    public IncidentType getType() {
        return IncidentType.MEDICAL;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.CRITICAL;
    }

    @Override
    public String requiredSkill() {
        return "medical";
    }

    @Override
    public long slaSeconds() {
        return 120;
    }
}
