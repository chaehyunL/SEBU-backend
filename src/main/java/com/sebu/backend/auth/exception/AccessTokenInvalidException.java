package com.sebu.backend.auth.exception;

public class AccessTokenInvalidException extends RuntimeException {
    public AccessTokenInvalidException() {
        super("ACCESS_TOKEN_INVALID");
    }
}
