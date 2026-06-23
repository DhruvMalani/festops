package com.festops.state;

import com.festops.exception.InvalidStateTransitionException;
import com.festops.model.Incident;

/**
 * Initial state of every incident. Can be acknowledged or escalated.
 */
public class ReportedState implements IncidentState {

    @Override
    public String name() {
        return "REPORTED";
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
        return new EscalatedState();
    }
}
