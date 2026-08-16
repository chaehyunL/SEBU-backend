package com.sebu.backend.application.crawling;

public interface ProfessorPageFetcher {
    FetchedProfessorPage fetch(String sourceUrl);
}
