package com.sebu.backend.bookmark.exception;

public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException() {
        super("INVALID_CURSOR");
    }
}
