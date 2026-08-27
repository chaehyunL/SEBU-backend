package com.sebu.backend.mypage.service;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.mypage.dto.MyPageResponse;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.professor.repository.ProfessorRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class MyPageServiceTest {
    @Autowired
    MyPageService myPageService;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    CollegeRepository collegeRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    ProfessorRepository professorRepository;

    @Autowired
    LaboratoryRepository laboratoryRepository;

    @Autowired
    BookmarkRepository bookmarkRepository;

    @Test
    void 마이페이지에서_프로필과_북마크_개수를_조회한다(){
        College college =collegeRepository.save(
                new College("소프트웨어융합대학")
        );
        Department department = departmentRepository.save(
                new Department(college, "컴퓨터공학과")
        );

        Professor professor = professorRepository.save(
                new Professor(department, "홍교수", null)
        );

        Laboratory laboratory = laboratoryRepository.save(
                new Laboratory(
                        professor,
                        department,
                        "AI 연구실",
                        null,
                        RecruitmentStatus.RECRUITING
                )
        );

        AppUser user = appUserRepository.save(
                new AppUser("student@example.com")
        );

        bookmarkRepository.save(
                new Bookmark(user, laboratory)
        );

        MyPageResponse result =
                myPageService.getMyPage(user.getId());
        assertThat(result.summary().bookmarkedLaboratoryCount())
                .isEqualTo(1);

        assertThat(result.bookmarkedLaboratories().items())
                .hasSize(1);

        assertThat(
                result.bookmarkedLaboratories()
                        .items()
                        .getFirst()
                        .laboratory()
                        .name()
        ).isEqualTo("AI 연구실");

        assertThat(
                result.bookmarkedLaboratories()
                        .items()
                        .getFirst()
                        .laboratory()
                        .bookmarked()
        ).isTrue();
    }
}
