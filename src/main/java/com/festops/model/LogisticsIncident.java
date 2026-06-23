package com.festops.model;

/**
 * A logistics/operations issue — lowest urgency, longest SLA, handled by the
 * maintenance crew.
 */
public class LogisticsIncident extends Incident {

    public LogisticsIncident(String id, String description, double lat, double lng) {
        super(id, description, lat, lng);
    }

    @Override
    public IncidentType getType() {
        return IncidentType.LOGISTICS;
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.LOW;
    }

    @Override
    public String requiredSkill() {
        return "maintenance";
    }

    @Override
    public long slaSeconds() {
        return 600;
    }
}
