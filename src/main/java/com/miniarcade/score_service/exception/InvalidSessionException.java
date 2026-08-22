package com.miniarcade.score_service.exception;

/** Session expired, or its gameType/difficulty doesn't match the submission. */
public class InvalidSessionException extends RuntimeException {
    public InvalidSessionException(String message) {
        super(message);
    }
}
