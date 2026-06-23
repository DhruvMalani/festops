package com.festops.state;

import com.festops.exception.InvalidStateTransitionException;
import com.festops.model.Incident;
import com.festops.model.MedicalIncident;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the incident lifecycle State pattern: every valid transition moves
 * to the expected state, and every invalid transition throws
 * {@link InvalidStateTransitionException}.
 */
class IncidentStateTransitionTest {

    /** A fresh incident always starts in REPORTED. */
    private Incident newIncident() {
        return new MedicalIncident("INC-1", "test", 0.0, 0.0);
    }

    private Incident inState(String target) {
        Incident incident = newIncident();
        switch (target) {
            case "REPORTED" -> { /* already there */ }
            case "ACKNOWLEDGED" -> incident.acknowledge();
            case "RESPONDING" -> { incident.acknowledge(); incident.startResponding(); }
            case "RESOLVED" -> { incident.acknowledge(); incident.startResponding(); incident.resolve(); }
            case "ESCALATED" -> incident.escalate();
            default -> throw new IllegalArgumentException(target);
        }
        assertEquals(target, incident.getStateName());
        return incident;
    }

    // --- valid transitions --------------------------------------------------

    @Test
    void startsInReported() {
        assertEquals("REPORTED", newIncident().getStateName());
    }

    @Test
    void reportedToAcknowledged() {
        Incident i = inState("REPORTED");
        i.acknowledge();
        assertEquals("ACKNOWLEDGED", i.getStateName());
    }

    @Test
    void acknowledgedToResponding() {
        Incident i = inState("ACKNOWLEDGED");
        i.startResponding();
        assertEquals("RESPONDING", i.getStateName());
    }

    @Test
    void respondingToResolved() {
        Incident i = inState("RESPONDING");
        i.resolve();
        assertEquals("RESOLVED", i.getStateName());
    }

    @Test
    void anyStateCanEscalate() {
        for (String state : new String[]{"REPORTED", "ACKNOWLEDGED", "RESPONDING", "RESOLVED"}) {
            Incident i = inState(state);
            i.escalate();
            assertEquals("ESCALATED", i.getStateName(),
                    "escalate() should be valid from " + state);
        }
    }

    @Test
    void escalatedToAcknowledged() {
        Incident i = inState("ESCALATED");
        i.acknowledge();
        assertEquals("ACKNOWLEDGED", i.getStateName());
    }

    @Test
    void fullHappyPath() {
        Incident i = newIncident();
        i.acknowledge();
        i.startResponding();
        i.resolve();
        assertEquals("RESOLVED", i.getStateName());
    }

    // --- invalid transitions (must throw) -----------------------------------

    @Test
    void reportedRejectsRespondAndResolve() {
        assertThrows(InvalidStateTransitionException.class, () -> inState("REPORTED").startResponding());
        assertThrows(InvalidStateTransitionException.class, () -> inState("REPORTED").resolve());
    }

    @Test
    void acknowledgedRejectsAcknowledgeAndResolve() {
        assertThrows(InvalidStateTransitionException.class, () -> inState("ACKNOWLEDGED").acknowledge());
        assertThrows(InvalidStateTransitionException.class, () -> inState("ACKNOWLEDGED").resolve());
    }

    @Test
    void respondingRejectsAcknowledgeAndRespond() {
        assertThrows(InvalidStateTransitionException.class, () -> inState("RESPONDING").acknowledge());
        assertThrows(InvalidStateTransitionException.class, () -> inState("RESPONDING").startResponding());
    }

    @Test
    void resolvedRejectsEverythingButEscalate() {
        assertThrows(InvalidStateTransitionException.class, () -> inState("RESOLVED").acknowledge());
        assertThrows(InvalidStateTransitionException.class, () -> inState("RESOLVED").startResponding());
        assertThrows(InvalidStateTransitionException.class, () -> inState("RESOLVED").resolve());
    }

    @Test
    void escalatedRejectsRespondResolveAndReEscalate() {
        assertThrows(InvalidStateTransitionException.class, () -> inState("ESCALATED").startResponding());
        assertThrows(InvalidStateTransitionException.class, () -> inState("ESCALATED").resolve());
        assertThrows(InvalidStateTransitionException.class, () -> inState("ESCALATED").escalate());
    }
}
