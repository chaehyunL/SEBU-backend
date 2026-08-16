package com.sebu.backend.application.crawling;

import com.sebu.backend.domain.crawling.CrawlSource;
import com.sebu.backend.domain.crawling.CrawlSourceProvenance;

import java.util.Objects;

public record CrawlSourceRun(
    Long sourceId,
    String sourceName,
    CrawlSourceProvenance provenance
) {
    public CrawlSourceRun {
        sourceId = Objects.requireNonNull(sourceId, "SOURCE_ID_REQUIRED");
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("SOURCE_NAME_REQUIRED");
        }
        sourceName = sourceName.trim();
        provenance = Objects.requireNonNull(provenance, "CRAWL_PROVENANCE_REQUIRED");
    }

    public static CrawlSourceRun from(CrawlSource source) {
        Objects.requireNonNull(source, "SOURCE_REQUIRED");
        return new CrawlSourceRun(
            source.getId(),
            source.getSourceName(),
            CrawlSourceProvenance.from(source)
        );
    }
}
