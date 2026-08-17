package com.sebu.backend.laboratory.service;

import com.sebu.backend.laboratory.service.LaboratoryManagementService;
import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.professor.repository.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LaboratoryManagementServiceTest {
    @Autowired CollegeRepository collegeRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ProfessorRepository professorRepository;
    @Autowired LaboratoryManagementService service;

    @Test
    void rejectsProfessorDepartmentMismatch() {
        College college = collegeRepository.save(new College("서비스테스트 인공지능융합대학"));
        Department ai = departmentRepository.save(new Department(college, "인공지능학과"));
        Department computer = departmentRepository.save(new Department(college, "컴퓨터공학과"));
        Professor professor = professorRepository.save(new Professor(ai, "김교수", null));
        assertThatThrownBy(() -> service.create(professor.getId(), computer.getId(), "연구실", null, RecruitmentStatus.RECRUITING))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("PROFESSOR_DEPARTMENT_MISMATCH");
    }
}
