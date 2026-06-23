package com.festops.state;

import com.festops.exception.InvalidStateTransitionException;
import com.festops.model.Incident;

/**
 * The incident is resolved. Terminal except that it may still be escalated
 * (the {@code any→Escalated} rule).
 */
public class ResolvedState implements IncidentState {

    @Override
    public String name() {
        return "RESOLVED";
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
        throw new InvalidStateTransitionException(name(), "resolve");
    }

    @Override
    public IncidentState escalate(Incident incident) {
        return new EscalatedState();
    }
}
