package com.sebu.backend.laboratory.controller;

import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.laboratory.service.LaboratoryQueryService;
import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.domain.LaboratoryResearchField;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.professor.repository.ProfessorRepository;
import com.sebu.backend.researchfield.domain.ResearchField;
import com.sebu.backend.researchfield.repository.ResearchFieldRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LaboratoryApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired CollegeRepository collegeRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ProfessorRepository professorRepository;
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired ResearchFieldRepository researchFieldRepository;
    @Autowired LaboratoryResearchFieldRepository laboratoryResearchFieldRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired BookmarkRepository bookmarkRepository;
    @Autowired LaboratoryQueryService queryService;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @MockitoBean CurrentUserProvider currentUserProvider;

    private Long firstUserId;

    @BeforeEach
    void setUp() {
        College college = collegeRepository.save(new College("API테스트 인공지능융합대학"));
        Department ai = departmentRepository.save(new Department(college, "인공지능학과"));
        Department computer = departmentRepository.save(new Department(college, "컴퓨터공학과"));
        Professor kim = professorRepository.save(new Professor(ai, "김민준", "minjun.kim@example.ac.kr"));
        Professor park = professorRepository.save(new Professor(computer, "박지훈", null));
        Laboratory lab1 = laboratoryRepository.save(new Laboratory(kim, ai, "인공지능연구실", "https://ai-lab.example.ac.kr", RecruitmentStatus.RECRUITING));
        laboratoryRepository.save(new Laboratory(park, computer, "연구분야없는연구실", null, RecruitmentStatus.ALWAYS_OPEN));
        Laboratory deleted = laboratoryRepository.save(new Laboratory(park, computer, "삭제된연구실", null, RecruitmentStatus.CLOSED));
        deleted.softDelete();
        ResearchField machineLearning = researchFieldRepository.save(new ResearchField("머신러닝"));
        ResearchField aiField = researchFieldRepository.save(new ResearchField("인공지능"));
        laboratoryResearchFieldRepository.save(new LaboratoryResearchField(lab1, machineLearning));
        laboratoryResearchFieldRepository.save(new LaboratoryResearchField(lab1, aiField));
        AppUser user1 = appUserRepository.save(new AppUser("one@example.com"));
        AppUser user2 = appUserRepository.save(new AppUser("two@example.com"));
        bookmarkRepository.save(new Bookmark(user1, lab1));
        bookmarkRepository.save(new Bookmark(user2, lab1));
        firstUserId = user1.getId();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void returnsAuthenticatedLaboratoryResponseMatchingSpecification() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(firstUserId));
        mockMvc.perform(get("/api/v1/laboratories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.data.laboratories.length()").value(2))
            .andExpect(jsonPath("$.data.laboratories[0].name").value("인공지능연구실"))
            .andExpect(jsonPath("$.data.laboratories[0].professor.name").value("김민준"))
            .andExpect(jsonPath("$.data.laboratories[0].college.name").value("API테스트 인공지능융합대학"))
            .andExpect(jsonPath("$.data.laboratories[0].department.name").value("인공지능학과"))
            .andExpect(jsonPath("$.data.laboratories[0].researchFields.length()").value(2))
            .andExpect(jsonPath("$.data.laboratories[0].bookmarkCount").value(2))
            .andExpect(jsonPath("$.data.laboratories[0].bookmarked").value(true))
            .andExpect(jsonPath("$.data.laboratories[1].websiteUrl").doesNotExist())
            .andExpect(jsonPath("$.data.laboratories[1].professor.email").doesNotExist())
            .andExpect(jsonPath("$.data.laboratories[1].researchFields").isEmpty())
            .andExpect(jsonPath("$.data.laboratories[1].bookmarkCount").value(0));
    }

    @Test
    void anonymousUserIsNeverBookmarked() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/laboratories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.laboratories[0].bookmarked").value(false));
    }

    @Test
    void queryCountStaysAtTwoWithoutNPlusOne() {
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(firstUserId));
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(queryService.getAll().laboratories()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
