package com.example.sfe4j.core.service;

public class RemoteAccessException extends RuntimeException {
    public RemoteAccessException(String message) {
        super(message);
    }

    public RemoteAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
