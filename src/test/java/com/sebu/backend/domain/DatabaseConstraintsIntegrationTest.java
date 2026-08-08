package com.sebu.backend.domain;

import com.sebu.backend.domain.bookmark.*;
import com.sebu.backend.domain.college.*;
import com.sebu.backend.domain.department.*;
import com.sebu.backend.domain.laboratory.*;
import com.sebu.backend.domain.professor.*;
import com.sebu.backend.domain.researchfield.*;
import com.sebu.backend.domain.user.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class DatabaseConstraintsIntegrationTest {
    @Autowired CollegeRepository collegeRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ProfessorRepository professorRepository;
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired ResearchFieldRepository researchFieldRepository;
    @Autowired LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired BookmarkRepository bookmarkRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void identityEnumAndResearchFieldOptionalityAreMapped() {
        Hierarchy h = hierarchy("AI", "인공지능학과", "김교수", null);
        Laboratory lab = laboratoryRepository.saveAndFlush(new Laboratory(h.professor, h.department, "AI 연구실", null, RecruitmentStatus.ALWAYS_OPEN));
        assertThat(lab.getId()).isNotNull();
        entityManager.clear();
        Laboratory found = laboratoryRepository.findById(lab.getId()).orElseThrow();
        assertThat(found.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.ALWAYS_OPEN);
        assertThat(found.getWebsiteUrl()).isNull();
        assertThat(laboratoryResearchFieldRepository.count()).isZero();
    }

    @Test
    void professorEmailIsUniqueButMultipleNullsAreAllowed() {
        College college = collegeRepository.save(new College("공과대학"));
        Department department = departmentRepository.save(new Department(college, "컴퓨터공학과"));
        professorRepository.save(new Professor(department, "교수1", null));
        professorRepository.save(new Professor(department, "교수2", null));
        professorRepository.flush();
        assertThat(professorRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateProfessorEmailIsRejectedAfterNormalization() {
        College college = collegeRepository.save(new College("융합대학"));
        Department department = departmentRepository.save(new Department(college, "소프트웨어학과"));
        professorRepository.saveAndFlush(new Professor(department, "교수1", "USER@EXAMPLE.COM"));
        assertThatThrownBy(() -> professorRepository.saveAndFlush(new Professor(department, "교수2", " user@example.com ")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateBookmarkAndResearchFieldMappingAreRejected() {
        Hierarchy h = hierarchy("자연대학", "수학과", "이교수", "lee@example.com");
        Laboratory lab = laboratoryRepository.saveAndFlush(new Laboratory(h.professor, h.department, "수리 연구실", null, RecruitmentStatus.RECRUITING));
        AppUser user = appUserRepository.saveAndFlush(new AppUser("student@example.com"));
        ResearchField field = researchFieldRepository.saveAndFlush(new ResearchField("최적화"));
        bookmarkRepository.saveAndFlush(new Bookmark(user, lab));
        laboratoryResearchFieldRepository.saveAndFlush(new LaboratoryResearchField(lab, field));
        assertThat(bookmarkRepository.existsById(new BookmarkId(user.getId(), lab.getId()))).isTrue();
        assertThat(laboratoryResearchFieldRepository.existsById(new LaboratoryResearchFieldId(lab.getId(), field.getId()))).isTrue();
    }

    @Test
    void referencedProfessorCannotBePhysicallyDeleted() {
        Hierarchy h = hierarchy("의과대학", "의학과", "박교수", "park@example.com");
        laboratoryRepository.saveAndFlush(new Laboratory(h.professor, h.department, "의학 연구실", null, RecruitmentStatus.CLOSED));
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM professor WHERE id = ?", h.professor.getId()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void physicalLaboratoryDeletionCascadesMappingsAndBookmarks() {
        Hierarchy h = hierarchy("생명대학", "생명과학과", "최교수", "choi@example.com");
        Laboratory lab = laboratoryRepository.saveAndFlush(new Laboratory(h.professor, h.department, "생명 연구실", null, RecruitmentStatus.RECRUITING));
        ResearchField field = researchFieldRepository.saveAndFlush(new ResearchField("바이오"));
        AppUser user = appUserRepository.saveAndFlush(new AppUser("bio@example.com"));
        laboratoryResearchFieldRepository.saveAndFlush(new LaboratoryResearchField(lab, field));
        bookmarkRepository.saveAndFlush(new Bookmark(user, lab));
        jdbcTemplate.update("DELETE FROM laboratory WHERE id = ?", lab.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM laboratory_research_field WHERE laboratory_id = ?", Long.class, lab.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bookmark WHERE laboratory_id = ?", Long.class, lab.getId())).isZero();
    }

    private Hierarchy hierarchy(String collegeName, String departmentName, String professorName, String email) {
        College college = collegeRepository.save(new College(collegeName));
        Department department = departmentRepository.save(new Department(college, departmentName));
        Professor professor = professorRepository.save(new Professor(department, professorName, email));
        return new Hierarchy(department, professor);
    }

    private record Hierarchy(Department department, Professor professor) { }
}
