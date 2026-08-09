package com.sebu.backend.domain;

import com.sebu.backend.application.laboratory.LaboratoryManagementService;
import com.sebu.backend.domain.college.*;
import com.sebu.backend.domain.department.*;
import com.sebu.backend.domain.laboratory.*;
import com.sebu.backend.domain.professor.*;
import com.sebu.backend.domain.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class LaboratoryManagementServiceTest {
    @Autowired CollegeRepository collegeRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ProfessorRepository professorRepository;
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired LaboratoryManagementService service;

    @Test
    void rejectsProfessorDepartmentMismatch() {
        College college = collegeRepository.save(new College("인공지능융합대학"));
        Department ai = departmentRepository.save(new Department(college, "인공지능학과"));
        Department computer = departmentRepository.save(new Department(college, "컴퓨터공학과"));
        Professor professor = professorRepository.save(new Professor(ai, "김교수", null));
        assertThatThrownBy(() -> service.create(professor.getId(), computer.getId(), "연구실", null, RecruitmentStatus.RECRUITING))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("PROFESSOR_DEPARTMENT_MISMATCH");
    }

    @Test
    void softDeletedLaboratoryIsNotBookmarkable() {
        College college = collegeRepository.save(new College("공과대학"));
        Department department = departmentRepository.save(new Department(college, "전자공학과"));
        Professor professor = professorRepository.save(new Professor(department, "이교수", null));
        Laboratory lab = service.create(professor.getId(), department.getId(), "전자 연구실", null, RecruitmentStatus.CLOSED);
        AppUser user = appUserRepository.save(new AppUser("student@example.com"));
        service.softDelete(lab.getId());
        assertThat(lab.isDeleted()).isTrue();
        assertThatThrownBy(() -> service.addBookmark(user.getId(), lab.getId()))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Laboratory not found");
    }
}
