package com.sebu.backend.mypage.service;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.college.domain.College;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldProjection;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.mypage.dto.MyPageResponse;
import com.sebu.backend.professor.domain.Professor;
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
    private final LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;

    public MyPageResponse getMyPage(Long userId) {

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        long bookmarkedLaboratoryCount =
                bookmarkRepository.countByUser_Id(userId);

        List<Bookmark> bookmarks =
                bookmarkRepository.findTop5ByUser_IdOrderByCreatedAtDesc(userId);

        boolean hasNext = bookmarkedLaboratoryCount > 5;

        List<Long> laboratoryIds = bookmarks.stream()
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

        MyPageResponse.Profile profile = toProfile(user);

        MyPageResponse.Summary summary =
                new MyPageResponse.Summary(bookmarkedLaboratoryCount);

        List<MyPageResponse.BookmarkedLaboratory> bookmarkedLaboratories =
                bookmarks.stream()
                        .map(bookmark ->
                                toBookmarkedLaboratory(
                                        bookmark,
                                        researchFieldsByLaboratoryId
                                )
                        )
                        .toList();

        return new MyPageResponse(
                profile,
                summary,
                new MyPageResponse.BookmarkedLaboratories(
                        bookmarkedLaboratories,
                        hasNext
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
            Map<Long,List<String>> researchFieldsByLaboratoryId
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
        Department department = laboratory.getDepartment();
        College college = department.getCollege();
        Professor professor = laboratory.getProfessor();

        MyPageResponse.CollegeSummary collegeSummary =
                new MyPageResponse.CollegeSummary(
                        college.getId().toString(),
                        college.getName()
                );

        MyPageResponse.DepartmentSummary departmentSummary =
                new MyPageResponse.DepartmentSummary(
                        department.getId().toString(),
                        department.getName()
                );
        MyPageResponse.ProfessorSummary professorSummary =
                new MyPageResponse.ProfessorSummary(
                        professor.getId().toString(),
                        professor.getName()
                );
        List<String> researchFields =
                researchFieldsByLaboratoryId.getOrDefault(
                        laboratory.getId(),
                        List.of()
                );

        long bookmarkCount =
                bookmarkRepository.countActiveByLaboratoryId(
                        laboratory.getId()
                );

        return new MyPageResponse.LaboratorySummary(
                laboratory.getId().toString(),
                laboratory.getName(),
                laboratory.getWebsiteUrl(),
                collegeSummary,
                departmentSummary,
                professorSummary,
                researchFields,
                laboratory.getRecruitmentStatus().name(),
                bookmarkCount,
                true
        );

    }
}
