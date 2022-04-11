package com.pattexpattex.servergods2.core.exceptions;

public class SpotifyException extends RuntimeException {
    public SpotifyException() {
        super();
    }

    public SpotifyException(String message) {
        super(message);
    }

    public SpotifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpotifyException(Throwable cause) {
        super(cause);
    }

    protected SpotifyException(String message, Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
