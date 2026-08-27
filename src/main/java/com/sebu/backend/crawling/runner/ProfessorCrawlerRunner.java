package com.sebu.backend.crawling.runner;

import com.sebu.backend.crawling.config.ProfessorCrawlerProperties;
import com.sebu.backend.crawling.dto.ProfessorCrawlBatchResult;
import com.sebu.backend.crawling.dto.ProfessorCrawlResult;
import com.sebu.backend.crawling.exception.ProfessorCrawlException;
import com.sebu.backend.crawling.service.ProfessorCrawlBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("crawler & !promotion")
@ConditionalOnProperty(
    prefix = "app.professor-crawler",
    name = "enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class ProfessorCrawlerRunner implements ApplicationRunner {
    private final ProfessorCrawlBatchService batchService;
    private final ProfessorCrawlerProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        ProfessorCrawlBatchResult batchResult = batchService.crawl(
            properties.getSourceId(),
            properties.getRequestDelay()
        );
        for (ProfessorCrawlResult result : batchResult.successes()) {
            log.info(
                "Professor crawl succeeded: sourceId={}, sourceName={}, crawled={}, created={}, refreshed={}, stale={}",
                result.sourceId(),
                result.sourceName(),
                result.crawledCount(),
                result.createdCount(),
                result.refreshedCount(),
                result.staleCount()
            );
        }
        for (ProfessorCrawlBatchResult.Failure failure : batchResult.failures()) {
            log.error(
                "Professor crawl failed: sourceId={}, sourceName={}, reason={}",
                failure.sourceId(),
                failure.sourceName(),
                failure.reason(),
                failure.exception()
            );
        }

        log.info(
            "Professor crawl finished: sources={}, succeeded={}, failed={}, candidates={}",
            batchResult.sourceCount(),
            batchResult.successes().size(),
            batchResult.failures().size(),
            batchResult.totalCandidateCount()
        );
        if (batchResult.hasFailures()) {
            throw new ProfessorCrawlException(
                "PROFESSOR_CRAWL_PARTIALLY_FAILED: " + batchResult.failures().size(),
                batchResult.failures().getFirst().exception()
            );
        }
    }
}
