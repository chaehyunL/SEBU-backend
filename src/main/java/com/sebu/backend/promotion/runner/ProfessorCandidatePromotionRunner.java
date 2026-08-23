package com.sebu.backend.promotion.runner;

import com.sebu.backend.promotion.config.PromotionProperties;
import com.sebu.backend.promotion.dto.PromotionResult;
import com.sebu.backend.promotion.exception.CandidatePromotionException;
import com.sebu.backend.promotion.service.ProfessorCandidatePromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("promotion & !crawler")
@ConditionalOnProperty(
    prefix = "app.candidate-promotion",
    name = "enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class ProfessorCandidatePromotionRunner implements ApplicationRunner {
    private final ProfessorCandidatePromotionService promotionService;
    private final PromotionProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        PromotionResult result = promotionService.promote(properties.getSourceId());
        for (PromotionResult.Failure failure : result.failures()) {
            log.error(
                "Professor candidate promotion failed: candidateId={}, reason={}",
                failure.candidateId(),
                failure.reason(),
                failure.exception()
            );
        }
        log.info(
            "Professor candidate promotion finished: candidates={}, created={}, updated={}, skipped={}, failed={}",
            result.candidateCount(),
            result.createdCount(),
            result.updatedCount(),
            result.skippedCount(),
            result.failedCount()
        );
        if (result.hasFailures()) {
            throw new CandidatePromotionException(
                "CANDIDATE_PROMOTION_PARTIALLY_FAILED: " + result.failedCount(),
                result.failures().getFirst().exception()
            );
        }
    }
}
