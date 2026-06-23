package com.festops.model;

/**
 * The category of an incident. Drives which {@link Incident} subclass is built.
 */
public enum IncidentType {
    MEDICAL,
    SECURITY,
    FIRE,
    LOGISTICS
}
