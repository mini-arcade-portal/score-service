package com.miniarcade.score_service.exception;

public class SessionAlreadyUsedException extends RuntimeException {
    public SessionAlreadyUsedException(String message) {
        super(message);
    }
}
