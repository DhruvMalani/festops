package com.festops.state;

import com.festops.exception.InvalidStateTransitionException;
import com.festops.model.Incident;

/**
 * The incident has been acknowledged by ops. Can move to responding or escalate.
 */
public class AcknowledgedState implements IncidentState {

    @Override
    public String name() {
        return "ACKNOWLEDGED";
    }

    @Override
    public IncidentState acknowledge(Incident incident) {
        throw new InvalidStateTransitionException(name(), "acknowledge");
    }

    @Override
    public IncidentState respond(Incident incident) {
        return new RespondingState();
    }

    @Override
    public IncidentState resolve(Incident incident) {
        throw new InvalidStateTransitionException(name(), "resolve");
    }

    @Override
    public IncidentState escalate(Incident incident) {
        return new EscalatedState();
    }
}
