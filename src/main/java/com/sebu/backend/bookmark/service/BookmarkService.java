package com.sebu.backend.bookmark.service;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import com.sebu.backend.bookmark.dto.BookmarkedLaboratoriesResponse;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.mypage.dto.MyPageResponse;
import com.sebu.backend.user.domain.AppUser;
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

    @Transactional
    public void add(Long userId, Long laboratoryId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        Laboratory laboratory = laboratoryRepository
                .findByIdAndDeletedAtIsNull(laboratoryId)
                .orElseThrow(() -> new IllegalArgumentException("LABORATORY_NOT_FOUND"));

        BookmarkId bookmarkId = new BookmarkId(userId, laboratoryId);

        if (bookmarkRepository.existsById(bookmarkId)) {
            return;
        }

        bookmarkRepository.save(
                new Bookmark(user, laboratory)
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
                throw new IllegalArgumentException("INVALID_CURSOR");
            }

            return new CursorValue(
                    LocalDateTime.parse(parts[0]),
                    Long.parseLong(parts[1])
            );

        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_CURSOR");
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

    private MyPageResponse.BookmarkedLaboratory toBookmarkedLaboratory(
            Bookmark bookmark,
            Map<Long, List<String>> researchFieldsByLaboratoryId
    ) {
        return new MyPageResponse.BookmarkedLaboratory(
                bookmark.getCreatedAt(),
                toLaboratorySummary(
                        bookmark.getLaboratory(),
                        researchFieldsByLaboratoryId
                )
        );
    }

    private MyPageResponse.LaboratorySummary toLaboratorySummary(
            Laboratory laboratory,
            Map<Long, List<String>> researchFieldsByLaboratoryId
    ) {
        var department = laboratory.getDepartment();
        var college = department.getCollege();
        var professor = laboratory.getProfessor();

        List<String> researchFields =
                researchFieldsByLaboratoryId.getOrDefault(
                        laboratory.getId(),
                        List.of()
                );

        long bookmarkCount =
                bookmarkRepository.countByLaboratory_Id(
                        laboratory.getId()
                );

        return new MyPageResponse.LaboratorySummary(
                laboratory.getId().toString(),
                laboratory.getName(),
                laboratory.getWebsiteUrl(),
                new MyPageResponse.CollegeSummary(
                        college.getId().toString(),
                        college.getName()
                ),
                new MyPageResponse.DepartmentSummary(
                        department.getId().toString(),
                        department.getName()
                ),
                new MyPageResponse.ProfessorSummary(
                        professor.getId().toString(),
                        professor.getName()
                ),
                researchFields,
                laboratory.getRecruitmentStatus().name(),
                bookmarkCount,
                true
        );
    }

    private record CursorValue(
            LocalDateTime createdAt,
            Long laboratoryId
    ) {
    }

    @Transactional(readOnly = true)
    public BookmarkedLaboratoriesResponse getBookmarkedLaboratories(
            Long userId,
            String cursor,
            int size
    ) {
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("INVALID_SIZE");
        }

        CursorValue cursorValue = decodeCursor(cursor);

        System.out.println("=== CURSOR ===");
        System.out.println("cursor time = " + cursorValue.createdAt());
        System.out.println("cursor labId = " + cursorValue.laboratoryId());

        List<Bookmark> results =
                bookmarkRepository.findBookmarkedLaboratories(
                        userId,
                        cursorValue.createdAt(),
                        cursorValue.laboratoryId(),
                        PageRequest.of(0, size + 1)
                );

        System.out.println("=== QUERY RESULTS ===");
        results.forEach(bookmark ->
                System.out.println(
                        "result time = " + bookmark.getCreatedAt()
                                + " / labId = " + bookmark.getLaboratory().getId()
                )
        );

        boolean hasNext = results.size() > size;

        List<Bookmark> pageItems = hasNext
                ? results.subList(0, size)
                : results;

        List<Long> laboratoryIds = pageItems.stream()
                .map(bookmark -> bookmark.getLaboratory().getId())
                .toList();

        Map<Long, List<String>> researchFieldsByLaboratoryId =
                laboratoryIds.isEmpty()
                        ? Map.of()
                        : laboratoryResearchFieldRepository
                        .findFieldsByLaboratoryIds(laboratoryIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                LaboratoryResearchFieldProjection::getLaboratoryId,
                                Collectors.mapping(
                                        LaboratoryResearchFieldProjection::getName,
                                        Collectors.toList()
                                )
                        ));

        List<MyPageResponse.BookmarkedLaboratory> items =
                pageItems.stream()
                        .map(bookmark ->
                                toBookmarkedLaboratory(
                                        bookmark,
                                        researchFieldsByLaboratoryId
                                )
                        )
                        .toList();

        String nextCursor = hasNext && !pageItems.isEmpty()
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
        laboratoryRepository.findByIdAndDeletedAtIsNull(laboratoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException("LABORATORY_NOT_FOUND"));

        BookmarkId bookmarkId =
                new BookmarkId(userId, laboratoryId);

        if (!bookmarkRepository.existsById(bookmarkId)) {
            return;
        }

        bookmarkRepository.deleteById(bookmarkId);
    }
}
