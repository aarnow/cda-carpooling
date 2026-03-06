package com.cda.carpooling.exception.business;

/**
 * Levée quand il n'y a plus de places disponibles sur un trajet.
 */
public class NoSeatsAvailableException extends BusinessException {
    public NoSeatsAvailableException(String message) {
        super(message);
    }
}