package com.sebu.backend.researchfield.manualsplit.runner;

import com.sebu.backend.researchfield.manualsplit.config.ManualSplitImportProperties;
import com.sebu.backend.researchfield.manualsplit.dto.ManualSplitCsvRow;
import com.sebu.backend.researchfield.manualsplit.dto.ManualSplitImportResult;
import com.sebu.backend.researchfield.manualsplit.reader.ManualSplitCsvReader;
import com.sebu.backend.researchfield.manualsplit.service.ManualSplitImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile(
    "research-field-manual-split"
        + " & !crawler"
        + " & !promotion"
        + " & !research-field-extraction"
)
@ConditionalOnProperty(
    prefix = "app.research-field-manual-split",
    name = "enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class ManualSplitImportRunner implements ApplicationRunner {
    private final ManualSplitCsvReader csvReader;
    private final ManualSplitImportService importService;
    private final ManualSplitImportProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        List<ManualSplitCsvRow> rows = csvReader.read(properties.getCsvPath());
        ManualSplitImportResult result = importService.importRows(
            rows,
            properties.getReviewer()
        );
        log.info(
            "Manual research field split import finished: sources={}, rows={}, created={}, unchanged={}, rejectedSources={}",
            result.sourceCount(),
            result.rowCount(),
            result.createdCount(),
            result.unchangedCount(),
            result.rejectedSourceCount()
        );
    }
}
