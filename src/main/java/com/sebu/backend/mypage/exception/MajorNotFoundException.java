package com.sebu.backend.mypage.exception;

public class MajorNotFoundException extends RuntimeException {

    public MajorNotFoundException() {
        super("MAJOR_NOT_FOUND");
    }
}
