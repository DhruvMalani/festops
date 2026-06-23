package com.festops.exception;

/**
 * Thrown when an incident id is requested that does not exist.
 */
public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(String id) {
        super("No incident found with id '" + id + "'");
    }
}
