package com.paasmart.backend.exception;

public class BadRequestExceprion extends RuntimeException {
    public BadRequestExceprion(String message) {
        super(message);
    }
}
