package com.festops.state;

import com.festops.exception.InvalidStateTransitionException;
import com.festops.model.Incident;

/**
 * The incident has been escalated. The only legal move is back to
 * acknowledged ({@code Escalated→Acknowledged}).
 */
public class EscalatedState implements IncidentState {

    @Override
    public String name() {
        return "ESCALATED";
    }

    @Override
    public IncidentState acknowledge(Incident incident) {
        return new AcknowledgedState();
    }

    @Override
    public IncidentState respond(Incident incident) {
        throw new InvalidStateTransitionException(name(), "respond");
    }

    @Override
    public IncidentState resolve(Incident incident) {
        throw new InvalidStateTransitionException(name(), "resolve");
    }

    @Override
    public IncidentState escalate(Incident incident) {
        throw new InvalidStateTransitionException(name(), "escalate");
    }
}
