package com.cda.carpooling.exception.business;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(message);
    }
}