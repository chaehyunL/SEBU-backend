package com.sebu.backend.laboratory.scheduler;

import com.sebu.backend.laboratory.service.LaboratoryPurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LaboratoryPurgeScheduler {
    private final LaboratoryPurgeService purgeService;

    @Scheduled(
        cron = "${app.laboratory-retention.purge-cron:0 0 3 * * *}",
        zone = "${app.laboratory-retention.time-zone:Asia/Seoul}"
    )
    public void purgeExpiredLaboratories() {
        purgeService.purgeExpiredLaboratories(LocalDateTime.now());
    }
}
