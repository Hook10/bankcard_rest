package com.example.bankcards.exception;

public class InvalidCardOperationException extends RuntimeException {

    public InvalidCardOperationException(String message) {
        super(message);
    }
}
