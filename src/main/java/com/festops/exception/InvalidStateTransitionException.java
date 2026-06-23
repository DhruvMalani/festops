package com.festops.exception;

/**
 * Thrown when a lifecycle transition is attempted that the current incident
 * state does not allow.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String fromState, String action) {
        super("Invalid transition: cannot '" + action + "' from state '" + fromState + "'");
    }
}
