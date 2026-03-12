package com.cda.carpooling.exception.business;

/**
 * Levée quand un trajet n'est pas disponible pour réservation
 * (annulé, terminé, etc.).
 */
public class TripNotAvailableException extends BusinessException {
    public TripNotAvailableException(String message) {
        super(message);
    }
}