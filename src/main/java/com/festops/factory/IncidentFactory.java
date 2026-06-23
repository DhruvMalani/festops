package com.festops.factory;

import com.festops.model.FireIncident;
import com.festops.model.Incident;
import com.festops.model.IncidentType;
import com.festops.model.LogisticsIncident;
import com.festops.model.MedicalIncident;
import com.festops.model.SecurityIncident;

/**
 * Builds the correct {@link Incident} subclass for a given {@link IncidentType}.
 */
public final class IncidentFactory {

    private IncidentFactory() {
    }

    public static Incident create(IncidentType type, String id, String description,
                                  double lat, double lng) {
        return switch (type) {
            case MEDICAL -> new MedicalIncident(id, description, lat, lng);
            case SECURITY -> new SecurityIncident(id, description, lat, lng);
            case FIRE -> new FireIncident(id, description, lat, lng);
            case LOGISTICS -> new LogisticsIncident(id, description, lat, lng);
        };
    }
}
