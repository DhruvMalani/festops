package com.festops.exception;

/**
 * Thrown by the dispatcher when no available responder can be assigned to an
 * incident.
 */
public class NoResponderAvailableException extends RuntimeException {

    public NoResponderAvailableException(String incidentId) {
        super("No available responder for incident '" + incidentId + "'");
    }
}
