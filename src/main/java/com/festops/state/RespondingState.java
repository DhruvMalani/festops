package com.festops.state;

import com.festops.exception.InvalidStateTransitionException;
import com.festops.model.Incident;

/**
 * A responder is actively handling the incident. Can be resolved or escalated.
 */
public class RespondingState implements IncidentState {

    @Override
    public String name() {
        return "RESPONDING";
    }

    @Override
    public IncidentState acknowledge(Incident incident) {
        throw new InvalidStateTransitionException(name(), "acknowledge");
    }

    @Override
    public IncidentState respond(Incident incident) {
        throw new InvalidStateTransitionException(name(), "respond");
    }

    @Override
    public IncidentState resolve(Incident incident) {
        return new ResolvedState();
    }

    @Override
    public IncidentState escalate(Incident incident) {
        return new EscalatedState();
    }
}
