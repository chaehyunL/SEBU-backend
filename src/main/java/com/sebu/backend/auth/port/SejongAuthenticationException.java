package com.sebu.backend.auth.port;

import lombok.Getter;

@Getter
public class SejongAuthenticationException extends RuntimeException {
    private final Reason reason;

    private SejongAuthenticationException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public static SejongAuthenticationException authenticationFailed() {
        return new SejongAuthenticationException(
            Reason.AUTHENTICATION_FAILED,
            "SEJONG_AUTHENTICATION_FAILED",
            null
        );
    }

    public static SejongAuthenticationException systemUnavailable() {
        return systemUnavailable(null);
    }

    public static SejongAuthenticationException systemUnavailable(Throwable cause) {
        return new SejongAuthenticationException(
            Reason.SYSTEM_UNAVAILABLE,
            "SEJONG_SYSTEM_UNAVAILABLE",
            cause
        );
    }

    public enum Reason {
        AUTHENTICATION_FAILED,
        SYSTEM_UNAVAILABLE
    }
}
