package com.festops.dispatch;

import com.festops.exception.NoResponderAvailableException;
import com.festops.model.Incident;
import com.festops.model.MedicalIncident;
import com.festops.model.Responder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies dispatcher scoring (skill match, proximity) and the no-responder
 * failure mode.
 */
class DispatcherTest {

    /** Medical incident at the origin — requiredSkill "medical". */
    private Incident incident() {
        return new MedicalIncident("INC-1", "someone fainted", 0.0, 0.0);
    }

    @Test
    void skillMatchScoresHigherThanNonMatch() {
        Dispatcher dispatcher = new Dispatcher();
        Incident incident = incident();

        // identical location and load — only the skill differs
        Responder matching = new Responder("R1", "Medic", "medical", 0.0, 0.0, true, 0);
        Responder nonMatching = new Responder("R2", "Fire", "fire", 0.0, 0.0, true, 0);

        double matchScore = dispatcher.score(incident, matching);
        double nonMatchScore = dispatcher.score(incident, nonMatching);

        assertTrue(matchScore > nonMatchScore,
                "skill-matching responder should score higher (" + matchScore
                        + " vs " + nonMatchScore + ")");
    }

    @Test
    void closerResponderScoresHigher() {
        Dispatcher dispatcher = new Dispatcher();
        Incident incident = incident(); // at (0,0)

        // same skill and load — only distance differs
        Responder near = new Responder("R1", "Near", "medical", 0.0, 0.001, true, 0);  // ~111 m
        Responder far = new Responder("R2", "Far", "medical", 0.0, 0.05, true, 0);     // ~5.5 km

        double nearScore = dispatcher.score(incident, near);
        double farScore = dispatcher.score(incident, far);

        assertTrue(nearScore > farScore,
                "closer responder should score higher (" + nearScore + " vs " + farScore + ")");
    }

    @Test
    void lowerLoadScoresHigher() {
        Dispatcher dispatcher = new Dispatcher();
        Incident incident = incident();

        // same skill and location — only load differs
        Responder idle = new Responder("R1", "Idle", "medical", 0.0, 0.0, true, 0);
        Responder busy = new Responder("R2", "Busy", "medical", 0.0, 0.0, true, 5);

        assertTrue(dispatcher.score(incident, idle) > dispatcher.score(incident, busy),
                "less-loaded responder should score higher");
    }

    @Test
    void dispatchPicksHighestScoringAvailableResponder() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.register(new Responder("R1", "Medic", "medical", 0.0, 0.0, true, 0));
        dispatcher.register(new Responder("R2", "Fire", "fire", 0.0, 0.0, true, 0));

        Responder chosen = dispatcher.dispatch(incident());
        assertTrue("R1".equals(chosen.getId()), "should pick the skill-matching responder");
    }

    @Test
    void throwsWhenNoResponderRegistered() {
        Dispatcher dispatcher = new Dispatcher();
        assertThrows(NoResponderAvailableException.class, () -> dispatcher.dispatch(incident()));
    }

    @Test
    void throwsWhenAllResponderUnavailable() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.register(new Responder("R1", "Medic", "medical", 0.0, 0.0, false, 0));
        dispatcher.register(new Responder("R2", "Fire", "fire", 0.0, 0.0, false, 0));

        assertThrows(NoResponderAvailableException.class, () -> dispatcher.dispatch(incident()));
    }
}
