package com.sebu.backend.application.crawling;

import com.sebu.backend.domain.crawling.CrawlParserType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProfessorPageParserRegistry {
    private final Map<CrawlParserType, ProfessorPageParser> parsers;

    public ProfessorPageParserRegistry(List<ProfessorPageParser> parserList) {
        EnumMap<CrawlParserType, ProfessorPageParser> registered = new EnumMap<>(CrawlParserType.class);
        for (ProfessorPageParser parser : parserList) {
            ProfessorPageParser duplicate = registered.put(parser.supports(), parser);
            if (duplicate != null) {
                throw new IllegalStateException("DUPLICATE_PROFESSOR_PAGE_PARSER: " + parser.supports());
            }
        }
        parsers = Map.copyOf(registered);
    }

    public ProfessorPageParser get(CrawlParserType parserType) {
        ProfessorPageParser parser = parsers.get(parserType);
        if (parser == null) {
            throw new ProfessorCrawlException("UNSUPPORTED_CRAWL_PARSER: " + parserType);
        }
        return parser;
    }
}
