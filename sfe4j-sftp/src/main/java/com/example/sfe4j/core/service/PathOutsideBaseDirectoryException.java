package com.example.sfe4j.core.service;

public class PathOutsideBaseDirectoryException extends RuntimeException {
    public PathOutsideBaseDirectoryException(String message) {
        super(message);
    }
}
