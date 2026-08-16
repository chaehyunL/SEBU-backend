package com.sebu.backend.application.crawling;

import com.sebu.backend.domain.crawling.CrawlParserType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfessorPageParserRegistryTest {
    @Test
    void rejectsDuplicateParsersForTheSameParserType() {
        ProfessorPageParser first = parser(CrawlParserType.SEJONG_STANDARD);
        ProfessorPageParser duplicate = parser(CrawlParserType.SEJONG_STANDARD);

        assertThatThrownBy(() -> new ProfessorPageParserRegistry(List.of(first, duplicate)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("DUPLICATE_PROFESSOR_PAGE_PARSER: SEJONG_STANDARD");
    }

    private ProfessorPageParser parser(CrawlParserType parserType) {
        ProfessorPageParser parser = mock(ProfessorPageParser.class);
        when(parser.supports()).thenReturn(parserType);
        return parser;
    }
}
