package com.cda.carpooling.exception;

/**
 * Exception levée quand on tente de créer une ressource déjà existante.
 * Correspond à un HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s existe déjà avec %s : '%s'", resourceName, fieldName, fieldValue));
    }
}