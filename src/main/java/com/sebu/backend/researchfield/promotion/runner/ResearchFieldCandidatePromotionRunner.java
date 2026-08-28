package com.sebu.backend.researchfield.promotion.runner;

import com.sebu.backend.researchfield.promotion.config.ResearchFieldPromotionProperties;
import com.sebu.backend.researchfield.promotion.dto.ResearchFieldPromotionResult;
import com.sebu.backend.researchfield.promotion.exception.ResearchFieldPromotionException;
import com.sebu.backend.researchfield.promotion.service.ResearchFieldCandidatePromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile(
    "research-field-promotion"
        + " & !crawler"
        + " & !promotion"
        + " & !research-field-extraction"
        + " & !research-field-manual-split"
)
@ConditionalOnProperty(
    prefix = "app.research-field-promotion",
    name = "enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class ResearchFieldCandidatePromotionRunner implements ApplicationRunner {
    private final ResearchFieldCandidatePromotionService promotionService;
    private final ResearchFieldPromotionProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        ResearchFieldPromotionResult result = promotionService.promote(
            properties.getLaboratoryId()
        );
        for (ResearchFieldPromotionResult.Failure failure : result.failures()) {
            log.error(
                "Research field promotion failed: candidateId={}, reason={}",
                failure.candidateId(),
                failure.reason(),
                failure.exception()
            );
        }
        log.info(
            "Research field promotion finished: candidates={}, createdFields={}, createdLinks={}, promoted={}, skipped={}, failed={}",
            result.candidateCount(),
            result.createdFieldCount(),
            result.createdLinkCount(),
            result.promotedCount(),
            result.skippedCount(),
            result.failedCount()
        );
        if (result.hasFailures()) {
            throw new ResearchFieldPromotionException(
                "RESEARCH_FIELD_PROMOTION_PARTIALLY_FAILED: "
                    + result.failedCount(),
                result.failures().getFirst().exception()
            );
        }
    }
}
