package com.festops.triage;

import com.festops.model.IncidentType;

/**
 * Strategy for classifying free-text SOS reports into an {@link IncidentType}.
 */
public interface TriageStrategy {

    IncidentType classify(String sosText);
}
