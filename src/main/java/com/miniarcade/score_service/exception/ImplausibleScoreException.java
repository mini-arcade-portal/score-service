package com.miniarcade.score_service.exception;

public class ImplausibleScoreException extends RuntimeException {
    public ImplausibleScoreException(String message) {
        super(message);
    }
}
