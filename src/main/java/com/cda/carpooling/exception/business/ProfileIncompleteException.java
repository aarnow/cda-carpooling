package com.cda.carpooling.exception.business;

/**
 * Levée quand un utilisateur tente une action nécessitant un profil complété.
 */
public class ProfileIncompleteException extends BusinessException {
    public ProfileIncompleteException(String message) {
        super(message);
    }
}