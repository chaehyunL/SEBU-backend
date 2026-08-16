package com.sebu.backend.application.crawling;

public class ProfessorCrawlException extends RuntimeException {
    public ProfessorCrawlException(String message) {
        super(message);
    }

    public ProfessorCrawlException(String message, Throwable cause) {
        super(message, cause);
    }
}
