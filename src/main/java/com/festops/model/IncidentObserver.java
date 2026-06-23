package com.festops.model;

/**
 * Observer notified whenever an {@link Incident} changes lifecycle state.
 */
public interface IncidentObserver {

    /**
     * @param incident  the incident that transitioned
     * @param fromState name of the state left
     * @param toState   name of the state entered
     */
    void onStateChange(Incident incident, String fromState, String toState);
}
