package com.cda.carpooling.exception.business;

/**
 * Exception de base pour toutes les erreurs métier.
 * Les classes filles héritent de cette classe pour bénéficier
 * d'un traitement unifié dans le GlobalExceptionHandler.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}