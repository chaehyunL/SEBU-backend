package com.sebu.backend.researchfield.extraction.runner;

import com.sebu.backend.researchfield.extraction.config.ResearchFieldExtractionProperties;
import com.sebu.backend.researchfield.extraction.dto.ResearchFieldExtractionBatchResult;
import com.sebu.backend.researchfield.extraction.dto.ResearchFieldExtractionResult;
import com.sebu.backend.researchfield.extraction.exception.ResearchFieldExtractionException;
import com.sebu.backend.researchfield.extraction.service.ResearchFieldCandidateBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("research-field-extraction & !crawler & !promotion")
@ConditionalOnProperty(
    prefix = "app.research-field-extraction",
    name = "enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class ResearchFieldCandidateExtractionRunner implements ApplicationRunner {
    private final ResearchFieldCandidateBatchService batchService;
    private final ResearchFieldExtractionProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        ResearchFieldExtractionBatchResult batchResult = batchService.extract(
            properties.getLaboratoryId()
        );
        for (ResearchFieldExtractionResult result : batchResult.successes()) {
            log.info(
                "Research field extraction succeeded: laboratoryId={}, extracted={}, created={}, refreshed={}, stale={}, unchanged={}",
                result.laboratoryId(),
                result.extractedCount(),
                result.createdCount(),
                result.refreshedCount(),
                result.staleCount(),
                result.unchangedCount()
            );
        }
        for (ResearchFieldExtractionBatchResult.Failure failure : batchResult.failures()) {
            log.error(
                "Research field extraction failed: laboratoryId={}, reason={}",
                failure.laboratoryId(),
                failure.reason(),
                failure.exception()
            );
        }
        log.info(
            "Research field extraction finished: laboratories={}, succeeded={}, failed={}, created={}, refreshed={}, stale={}",
            batchResult.laboratoryCount(),
            batchResult.successes().size(),
            batchResult.failures().size(),
            batchResult.totalCreatedCount(),
            batchResult.totalRefreshedCount(),
            batchResult.totalStaleCount()
        );
        if (batchResult.hasFailures()) {
            throw new ResearchFieldExtractionException(
                "RESEARCH_FIELD_EXTRACTION_PARTIALLY_FAILED: "
                    + batchResult.failures().size(),
                batchResult.failures().getFirst().exception()
            );
        }
    }
}
