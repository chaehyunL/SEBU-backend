package com.sebu.backend.application.crawling;

public record FetchedProfessorPage(String html, String location) {
    public FetchedProfessorPage {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("PROFESSOR_PAGE_HTML_REQUIRED");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("PROFESSOR_PAGE_LOCATION_REQUIRED");
        }
    }
}
