package com.sebu.backend.domain;

import com.sebu.backend.application.bookmark.BookmarkService;
import com.sebu.backend.application.laboratory.LaboratoryManagementService;
import com.sebu.backend.domain.college.College;
import com.sebu.backend.domain.college.CollegeRepository;
import com.sebu.backend.domain.department.Department;
import com.sebu.backend.domain.department.DepartmentRepository;
import com.sebu.backend.domain.laboratory.Laboratory;
import com.sebu.backend.domain.laboratory.RecruitmentStatus;
import com.sebu.backend.domain.professor.Professor;
import com.sebu.backend.domain.professor.ProfessorRepository;
import com.sebu.backend.domain.user.AppUser;
import com.sebu.backend.domain.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookmarkServiceTest {
    @Autowired CollegeRepository collegeRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ProfessorRepository professorRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired LaboratoryManagementService laboratoryManagementService;
    @Autowired BookmarkService bookmarkService;

    @Test
    void softDeletedLaboratoryIsNotBookmarkable() {
        College college = collegeRepository.save(new College("공과대학"));
        Department department = departmentRepository.save(new Department(college, "전자공학과"));
        Professor professor = professorRepository.save(new Professor(department, "이교수", null));
        Laboratory laboratory = laboratoryManagementService.create(
            professor.getId(), department.getId(), "전자 연구실", null, RecruitmentStatus.CLOSED
        );
        AppUser user = appUserRepository.save(new AppUser("student@example.com"));

        laboratoryManagementService.softDelete(laboratory.getId());

        assertThat(laboratory.isDeleted()).isTrue();
        assertThatThrownBy(() -> bookmarkService.add(user.getId(), laboratory.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Laboratory not found");
    }
}
