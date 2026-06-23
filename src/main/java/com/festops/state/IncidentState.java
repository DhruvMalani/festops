package com.festops.state;

import com.festops.model.Incident;

/**
 * One state in the incident lifecycle (State pattern). Each method returns the
 * next state for a given action, or throws
 * {@link com.festops.exception.InvalidStateTransitionException} if the action is
 * not legal from this state.
 *
 * <p>Valid transitions:
 * Reported→Acknowledged, Acknowledged→Responding, Responding→Resolved,
 * any→Escalated, Escalated→Acknowledged.</p>
 */
public interface IncidentState {

    String name();

    IncidentState acknowledge(Incident incident);

    IncidentState respond(Incident incident);

    IncidentState resolve(Incident incident);

    IncidentState escalate(Incident incident);
}
