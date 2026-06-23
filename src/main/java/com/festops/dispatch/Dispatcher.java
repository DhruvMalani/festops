package com.festops.dispatch;

import com.festops.exception.NoResponderAvailableException;
import com.festops.model.Incident;
import com.festops.model.Responder;
import com.festops.util.HaversineUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scores available responders for an incident and assigns the best one.
 *
 * <p>Score = (skillMatch × 0.4) + (proximity × 0.3) + (loadScore × 0.3), where
 * proximity and loadScore are inverse-distance / inverse-load values in (0, 1].
 * Responders and assignments are held in {@link ConcurrentHashMap}s so the
 * dispatcher is safe to share across worker threads.</p>
 */
public class Dispatcher {

    private static final double SKILL_WEIGHT = 0.4;
    private static final double PROXIMITY_WEIGHT = 0.3;
    private static final double LOAD_WEIGHT = 0.3;

    private final Map<String, Responder> responders = new ConcurrentHashMap<>();
    private final Map<String, String> assignments = new ConcurrentHashMap<>();

    public void register(Responder responder) {
        responders.put(responder.getId(), responder);
    }

    /**
     * Composite suitability score for a responder against an incident.
     */
    public double score(Incident incident, Responder responder) {
        double skillMatch = responder.getSkill().equalsIgnoreCase(incident.requiredSkill()) ? 1.0 : 0.0;

        double distanceKm = HaversineUtil.distanceKm(
                incident.getLat(), incident.getLng(),
                responder.getLat(), responder.getLng());
        double proximity = 1.0 / (1.0 + distanceKm);

        double loadScore = 1.0 / (1.0 + responder.getCurrentLoad());

        return skillMatch * SKILL_WEIGHT
                + proximity * PROXIMITY_WEIGHT
                + loadScore * LOAD_WEIGHT;
    }

    /**
     * Picks the highest-scoring available responder, assigns the incident to it,
     * bumps that responder's load, and records the assignment.
     *
     * @return the chosen responder
     * @throws NoResponderAvailableException if no responder is available
     */
    public Responder dispatch(Incident incident) {
        Responder best = null;
        double bestScore = -1.0;

        for (Responder candidate : responders.values()) {
            if (!candidate.isAvailable()) {
                continue;
            }
            double s = score(incident, candidate);
            if (s > bestScore) {
                bestScore = s;
                best = candidate;
            }
        }

        if (best == null) {
            throw new NoResponderAvailableException(incident.getId());
        }
        best.incrementLoad();
        incident.setAssignedResponder(best);
        assignments.put(incident.getId(), best.getId());
        return best;
    }

    public Map<String, Responder> getResponders() {
        return responders;
    }

    public Map<String, String> getAssignments() {
        return assignments;
    }
}
