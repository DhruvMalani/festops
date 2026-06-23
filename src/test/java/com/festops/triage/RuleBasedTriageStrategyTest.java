package com.festops.triage;

import com.festops.factory.IncidentFactory;
import com.festops.model.Incident;
import com.festops.model.IncidentType;
import com.festops.model.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies regex-based triage classification and the resulting incident
 * severity for representative SOS phrases.
 */
class RuleBasedTriageStrategyTest {

    private final TriageStrategy triage = new RuleBasedTriageStrategy();

    /** Severity that an incident of the given type carries (via the factory). */
    private Severity severityFor(IncidentType type, String text) {
        Incident incident = IncidentFactory.create(type, "INC-1", text, 0.0, 0.0);
        return incident.getSeverity();
    }

    @Test
    void faintedMapsToMedicalCritical() {
        IncidentType type = triage.classify("someone fainted");
        assertEquals(IncidentType.MEDICAL, type);
        assertEquals(Severity.CRITICAL, severityFor(type, "someone fainted"));
    }

    @Test
    void theftMapsToSecurityHigh() {
        IncidentType type = triage.classify("theft");
        assertEquals(IncidentType.SECURITY, type);
        assertEquals(Severity.HIGH, severityFor(type, "theft"));
    }

    @Test
    void powerOutMapsToLogisticsLow() {
        IncidentType type = triage.classify("power out");
        assertEquals(IncidentType.LOGISTICS, type);
        assertEquals(Severity.LOW, severityFor(type, "power out"));
    }

    @Test
    void blankTextFallsBackToLogistics() {
        assertEquals(IncidentType.LOGISTICS, triage.classify(""));
        assertEquals(IncidentType.LOGISTICS, triage.classify(null));
    }
}
