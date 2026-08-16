package com.sebu.backend.application.crawling;

import com.sebu.backend.domain.crawling.CrawlParserType;
import com.sebu.backend.domain.crawling.ProfessorCrawlData;

import java.util.List;

public interface ProfessorPageParser {
    CrawlParserType supports();

    List<ProfessorCrawlData> parse(FetchedProfessorPage page);
}
