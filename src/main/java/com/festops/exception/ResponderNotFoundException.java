package com.festops.exception;

/**
 * Thrown when a responder id is requested that does not exist.
 */
public class ResponderNotFoundException extends RuntimeException {

    public ResponderNotFoundException(String id) {
        super("No responder found with id '" + id + "'");
    }
}
