package com.sebu.backend.mypage.service;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.laboratory.dto.LaboratoriesResult;
import com.sebu.backend.laboratory.query.LaboratorySummaryAssembler;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.laboratory.repository.LaboratorySummaryProjection;
import com.sebu.backend.mypage.dto.MyPageResponse;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.exception.UserNotFoundException;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final AppUserRepository appUserRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    private final LaboratorySummaryAssembler laboratorySummaryAssembler;

    public MyPageResponse getMyPage(Long userId) {

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        long bookmarkedLaboratoryCount =
                bookmarkRepository.countByUser_IdAndLaboratory_DeletedAtIsNull(userId);

        List<Bookmark> bookmarks =
                bookmarkRepository.findBookmarkedLaboratories(userId);

        List<Long> laboratoryIds = bookmarks.stream()
                .map(bookmark -> bookmark.getLaboratory().getId())
                .toList();

        Map<Long, LaboratorySummaryProjection> summaryByLaboratoryId =
                laboratoryIds.isEmpty()
                        ? Map.of()
                        : laboratoryRepository.findSummariesByIds(
                                userId,
                                laboratoryIds
                        ).stream()
                        .collect(Collectors.toMap(
                                LaboratorySummaryProjection::getId,
                                summary -> summary
                        ));

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

        MyPageResponse.Profile profile = toProfile(user);

        MyPageResponse.Summary summary =
                new MyPageResponse.Summary(bookmarkedLaboratoryCount);

        List<MyPageResponse.BookmarkedLaboratory> bookmarkedLaboratories =
                bookmarks.stream()
                        .map(bookmark ->
                                toBookmarkedLaboratory(
                                        bookmark,
                                        summaryByLaboratoryId,
                                        researchFieldsByLaboratoryId
                                )
                        )
                        .toList();

        return new MyPageResponse(
                profile,
                summary,
                new MyPageResponse.BookmarkedLaboratories(
                        bookmarkedLaboratories
                )
        );
    }

    private MyPageResponse.Profile toProfile(AppUser user) {
        MyPageResponse.DepartmentSummary department = null;

        if (user.getMajorDepartment() != null
                && user.getMajorDepartment().getName().equals(user.getSejongDepartmentName())) {
            department = new MyPageResponse.DepartmentSummary(
                    user.getMajorDepartment().getId().toString(),
                    user.getMajorDepartment().getName()
            );
        } else if (user.getSejongDepartmentName() != null) {
            department = new MyPageResponse.DepartmentSummary(
                    null,
                    user.getSejongDepartmentName()
            );
        }

        return new MyPageResponse.Profile(
                user.getName(),
                user.getNickname(),
                user.getGrade(),
                department,
                user.getGpaBand(),
                user.getIntroduction(),
                user.isProfileCompleted(),
                user.getProfileUpdatedAt()
        );
    }

    private MyPageResponse.BookmarkedLaboratory toBookmarkedLaboratory(
            Bookmark bookmark,
            Map<Long, LaboratorySummaryProjection> summaryByLaboratoryId,
            Map<Long, List<String>> researchFieldsByLaboratoryId
    ) {
        Long laboratoryId = bookmark.getLaboratory().getId();
        LaboratorySummaryProjection projection =
                summaryByLaboratoryId.get(laboratoryId);

        if (projection == null) {
            throw new IllegalStateException("LABORATORY_SUMMARY_NOT_FOUND");
        }

        LaboratoriesResult.LaboratoryResult laboratory =
                laboratorySummaryAssembler.assemble(
                        projection,
                        researchFieldsByLaboratoryId.getOrDefault(
                                laboratoryId,
                                List.of()
                        )
                );

        return new MyPageResponse.BookmarkedLaboratory(
                bookmark.getCreatedAt(),
                toLaboratorySummary(laboratory)
        );
    }

    private MyPageResponse.LaboratorySummary toLaboratorySummary(
            LaboratoriesResult.LaboratoryResult laboratory
    ) {
        MyPageResponse.CollegeSummary collegeSummary =
                new MyPageResponse.CollegeSummary(
                        laboratory.college().id().toString(),
                        laboratory.college().name()
                );

        MyPageResponse.DepartmentSummary departmentSummary =
                new MyPageResponse.DepartmentSummary(
                        laboratory.department().id().toString(),
                        laboratory.department().name()
                );
        MyPageResponse.ProfessorSummary professorSummary =
                new MyPageResponse.ProfessorSummary(
                        laboratory.professor().id().toString(),
                        laboratory.professor().name()
                );

        return new MyPageResponse.LaboratorySummary(
                laboratory.id().toString(),
                laboratory.name(),
                laboratory.websiteUrl(),
                collegeSummary,
                departmentSummary,
                professorSummary,
                laboratory.researchFields(),
                laboratory.recruitmentStatus().name(),
                laboratory.bookmarkCount(),
                laboratory.bookmarked()
        );

    }
}
