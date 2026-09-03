package com.sebu.backend.bookmark.service;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import com.sebu.backend.bookmark.dto.BookmarkedLaboratoriesResponse;
import com.sebu.backend.bookmark.exception.InvalidCursorException;
import com.sebu.backend.bookmark.exception.InvalidSizeException;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.exception.LaboratoryNotFoundException;
import com.sebu.backend.laboratory.query.LaboratorySummaryAssembler;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.laboratory.repository.LaboratorySummaryProjection;
import com.sebu.backend.user.exception.UserNotFoundException;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final AppUserRepository appUserRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    private final LaboratorySummaryAssembler laboratorySummaryAssembler;

    @Transactional
    public void add(Long userId, Long laboratoryId) {
        appUserRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        laboratoryRepository
                .findByIdAndDeletedAtIsNull(laboratoryId)
                .orElseThrow(LaboratoryNotFoundException::new);

        bookmarkRepository.insertIgnore(userId, laboratoryId);
    }

    @Transactional(readOnly = true)
    public BookmarkedLaboratoriesResponse getBookmarkedLaboratories(
            Long userId,
            String cursor,
            int size
    ) {
        if (size < 1 || size > 50) {
            throw new InvalidSizeException();
        }

        CursorValue cursorValue = decodeCursor(cursor);

        List<Bookmark> results =
                bookmarkRepository.findBookmarkedLaboratories(
                        userId,
                        cursorValue.createdAt(),
                        cursorValue.laboratoryId(),
                        PageRequest.of(0, size + 1)
                );

        boolean hasNext = results.size() > size;

        List<Bookmark> pageItems = hasNext
                ? results.subList(0, size)
                : results;

        List<Long> laboratoryIds = pageItems.stream()
                .map(bookmark ->
                        bookmark.getLaboratory().getId())
                .toList();

        Map<Long, LaboratorySummaryProjection> summaryByLaboratoryId =
                getLaboratorySummaries(
                        userId,
                        laboratoryIds
                );

        Map<Long, List<String>> researchFieldsByLaboratoryId =
                getResearchFields(laboratoryIds);

        List<BookmarkedLaboratoriesResponse.BookmarkedLaboratory> items =
                pageItems.stream()
                        .map(bookmark ->
                                toBookmarkedLaboratory(
                                        bookmark,
                                        summaryByLaboratoryId,
                                        researchFieldsByLaboratoryId
                                )
                        )
                        .toList();

        String nextCursor =
                hasNext && !pageItems.isEmpty()
                        ? encodeCursor(pageItems.getLast())
                        : null;

        return new BookmarkedLaboratoriesResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    @Transactional
    public void remove(Long userId, Long laboratoryId) {
        laboratoryRepository
                .findById(laboratoryId)
                .orElseThrow(LaboratoryNotFoundException::new);

        bookmarkRepository.deleteById(
                new BookmarkId(
                        userId,
                        laboratoryId
                )
        );
    }

    private Map<Long, LaboratorySummaryProjection> getLaboratorySummaries(
            Long userId,
            List<Long> laboratoryIds
    ) {
        if (laboratoryIds.isEmpty()) {
            return Map.of();
        }

        return laboratoryRepository
                .findSummariesByIds(
                        userId,
                        laboratoryIds
                )
                .stream()
                .collect(Collectors.toMap(
                        LaboratorySummaryProjection::getId,
                        summary -> summary
                ));
    }

    private Map<Long, List<String>> getResearchFields(
            List<Long> laboratoryIds
    ) {
        if (laboratoryIds.isEmpty()) {
            return Map.of();
        }

        return laboratoryResearchFieldRepository
                .findFieldsByLaboratoryIds(laboratoryIds)
                .stream()
                .collect(Collectors.groupingBy(
                        LaboratoryResearchFieldProjection::getLaboratoryId,
                        Collectors.mapping(
                                LaboratoryResearchFieldProjection::getName,
                                Collectors.toList()
                        )
                ));
    }

    private BookmarkedLaboratoriesResponse.BookmarkedLaboratory toBookmarkedLaboratory(
            Bookmark bookmark,
            Map<Long, LaboratorySummaryProjection> summaryByLaboratoryId,
            Map<Long, List<String>> researchFieldsByLaboratoryId
    ) {
        Long laboratoryId =
                bookmark.getLaboratory().getId();

        LaboratorySummaryProjection projection =
                summaryByLaboratoryId.get(laboratoryId);

        if (projection == null) {
            throw new IllegalStateException(
                    "LABORATORY_SUMMARY_NOT_FOUND"
            );
        }

        List<String> researchFields =
                researchFieldsByLaboratoryId.getOrDefault(
                        laboratoryId,
                        List.of()
                );

        LaboratoriesResult.LaboratoryResult result =
                laboratorySummaryAssembler.assemble(
                        projection,
                        researchFields
                );

        return new BookmarkedLaboratoriesResponse.BookmarkedLaboratory(
                bookmark.getCreatedAt(),
                toLaboratorySummary(result)
        );
    }

    private BookmarkedLaboratoriesResponse.LaboratorySummary toLaboratorySummary(
            LaboratoriesResult.LaboratoryResult result
    ) {
        return new BookmarkedLaboratoriesResponse.LaboratorySummary(
                result.id().toString(),
                result.name(),
                result.websiteUrl(),

                new BookmarkedLaboratoriesResponse.CollegeSummary(
                        result.college().id().toString(),
                        result.college().name()
                ),

                new BookmarkedLaboratoriesResponse.DepartmentSummary(
                        result.department().id().toString(),
                        result.department().name()
                ),

                new BookmarkedLaboratoriesResponse.ProfessorSummary(
                        result.professor().id().toString(),
                        result.professor().name()
                ),

                result.researchFields(),
                result.recruitmentStatus().name(),
                result.bookmarkCount(),
                result.bookmarked()
        );
    }

    private CursorValue decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorValue(null, null);
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );

            String[] parts = decoded.split("\\|");

            if (parts.length != 2) {
                throw new InvalidCursorException();
            }

            return new CursorValue(
                    LocalDateTime.parse(parts[0]),
                    Long.parseLong(parts[1])
            );

        } catch (InvalidCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCursorException();
        }
    }

    private String encodeCursor(Bookmark bookmark) {
        String value =
                bookmark.getCreatedAt()
                        + "|"
                        + bookmark.getLaboratory().getId();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        value.getBytes(StandardCharsets.UTF_8)
                );
    }

    private record CursorValue(
            LocalDateTime createdAt,
            Long laboratoryId
    ) {
    }
}
