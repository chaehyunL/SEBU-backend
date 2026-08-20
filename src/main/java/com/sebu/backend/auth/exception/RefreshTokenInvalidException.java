package com.sebu.backend.auth.exception;

public class RefreshTokenInvalidException extends RuntimeException {
    public RefreshTokenInvalidException() {
        super("REFRESH_TOKEN_INVALID");
    }
}
