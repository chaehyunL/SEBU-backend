package com.sebu.backend.application.crawling;

public record ProfessorCrawlResult(
    Long sourceId,
    String sourceName,
    int crawledCount,
    int createdCount,
    int refreshedCount,
    int staleCount
) {
}
