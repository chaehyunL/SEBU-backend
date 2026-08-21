package com.sebu.backend.mypage.controller;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.mypage.moderation.IntroductionModerationUnavailableException;
import com.sebu.backend.mypage.moderation.IntroductionModerator;
import com.sebu.backend.mypage.moderation.ModerationResult;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MyPageControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    CollegeRepository collegeRepository;
    @Autowired
    DepartmentRepository departmentRepository;
    @MockitoBean
    IntroductionModerator introductionModerator;

    @BeforeEach
    void setUp() {
        when(introductionModerator.moderate(anyString()))
                .thenReturn(
                        new ModerationResult(
                                true,
                                "v1",
                                "test-provider"
                        )
                );
    }


    @Test
    void 로그인한_사용자는_마이페이지를_조회할_수_있다() throws Exception {
        AppUser user = appUserRepository.save(
                new AppUser("student@example.com")
        );

        mockMvc.perform(
                        get("/api/v1/users/me/mypage")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject(user.getId().toString())
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        "private, no-store"
                ))
                .andExpect(jsonPath("$.data.profile").exists())
                .andExpect(jsonPath("$.data.summary.bookmarkedLaboratoryCount")
                        .value(0))
                .andExpect(jsonPath("$.data.bookmarkedLaboratories.items")
                        .isArray())
                .andExpect(jsonPath("$.data.bookmarkedLaboratories.items")
                        .isEmpty())
                .andExpect(jsonPath("$.data.bookmarkedLaboratories.hasNext")
                        .value(false));
    }

    @Test
    void 로그인한_사용자는_프로필을_저장할_수_있다() throws Exception {
        AppUser user = appUserRepository.save(
                new AppUser("profile-controller@example.com")
        );

        College college = collegeRepository.save(
                new College("프로필컨트롤러대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "AI로봇학과")
        );

        String requestBody = """
                {
                  "name": "홍길동",
                  "grade": 3,
                  "majorId": "%s",
                  "gpaBand": "GTE_3_5",
                  "introduction": "머신러닝에 관심이 있습니다."
                }
                """.formatted(major.getId());

        mockMvc.perform(
                        put("/api/v1/users/me/profile")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())
                                        .claim("role", "USER")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        "private, no-store"
                ))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.grade").value(3))
                .andExpect(jsonPath("$.data.major.id")
                        .value(major.getId().toString()))
                .andExpect(jsonPath("$.data.major.name")
                        .value("AI로봇학과"))
                .andExpect(jsonPath("$.data.gpaBand")
                        .value("GTE_3_5"))
                .andExpect(jsonPath("$.data.introduction")
                        .value("머신러닝에 관심이 있습니다."))
                .andExpect(jsonPath("$.data.profileCompleted")
                        .value(true))
                .andExpect(jsonPath("$.data.profileUpdatedAt").exists());
    }

    @Test
    void 학년이_범위를_벗어나면_프로필_저장에_실패한다() throws Exception {
        AppUser user = appUserRepository.save(
                new AppUser("invalid-grade@example.com")
        );

        College college = collegeRepository.save(
                new College("검증테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "검증테스트학과")
        );

        String requestBody = """
                {
                  "name": "홍길동",
                  "grade": 5,
                  "majorId": "%s",
                  "gpaBand": "GTE_3_5",
                  "introduction": "머신러닝에 관심이 있습니다."
                }
                """.formatted(major.getId());

        mockMvc.perform(
                        put("/api/v1/users/me/profile")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())
                                        .claim("role", "USER")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void 자기소개_검사_시스템에_장애가_발생하면_503을_반환한다() throws Exception {
        AppUser user = appUserRepository.save(
                new AppUser("profile-moderation-error@example.com")
        );

        College college = collegeRepository.save(
                new College("모더레이션장애테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "모더레이션장애테스트학과")
        );

        when(introductionModerator.moderate("검사할 자기소개"))
                .thenThrow(new IntroductionModerationUnavailableException());

        String requestBody = """
                {
                  "name": "홍길동",
                  "grade": 3,
                  "majorId": "%s",
                  "gpaBand": "GTE_3_5",
                  "introduction": "검사할 자기소개"
                }
                """.formatted(major.getId());

        mockMvc.perform(
                        put("/api/v1/users/me/profile")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())
                                        .claim("role", "USER")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code")
                .value("CONTENT_MODERATION_UNAVAILABLE"));
    }

    @Test
    void 로그인한_사용자는_회원_탈퇴할_수_있다() throws Exception {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("withdraw@example.com")
        );

        // when
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())
                                        .claim("role", "USER")
                                ))
                )
                // then
                .andExpect(status().isNoContent());

        AppUser withdrawnUser = appUserRepository.findById(user.getId())
                .orElseThrow();

        assertThat(withdrawnUser.getDeletedAt()).isNotNull();
        assertThat(withdrawnUser.isDeleted()).isTrue();
    }
    @Test
    void 탈퇴한_사용자는_기존_accessToken으로_마이페이지에_접근할_수_없다() throws Exception {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("withdraw-access@example.com")
        );

        user.withdraw();

        // when & then
        mockMvc.perform(
                        get("/api/v1/users/me/mypage")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())

                                        .claim("role", "USER")
                                ))
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void 자기소개가_정책에_위반되면_422를_반환하고_프로필은_변경되지_않는다() throws Exception {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("profile-policy@example.com")
        );

        College college = collegeRepository.save(
                new College("정책테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "정책테스트학과")
        );

        when(introductionModerator.moderate("차 단.테-스 트 표현"))
                .thenReturn(
                        new ModerationResult(
                                false,
                                "v1",
                                "test-provider"
                        )
                );

        String requestBody = """
        {
          "name": "홍길동",
          "grade": 3,
          "majorId": "%s",
          "gpaBand": "GTE_3_5",
          "introduction": "차 단.테-스 트 표현"
        }
        """.formatted(major.getId());

        mockMvc.perform(
                        put("/api/v1/users/me/profile")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())
                                        .claim("role", "USER")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code")
                        .value("CONTENT_POLICY_VIOLATION"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field")
                        .value("introduction"))
                .andExpect(jsonPath("$.error.fieldErrors[0].reason")
                        .value("INAPPROPRIATE_CONTENT"));

        AppUser savedUser = appUserRepository.findById(user.getId())
                .orElseThrow();

        assertThat(savedUser.getName()).isNull();
        assertThat(savedUser.getGrade()).isNull();
        assertThat(savedUser.getMajorDepartment()).isNull();
        assertThat(savedUser.getGpaBand()).isNull();
        assertThat(savedUser.getIntroduction()).isEmpty();
        assertThat(savedUser.getProfileUpdatedAt()).isNull();
    }

    @Test
    void 자기소개_검사_시스템에_장애가_발생하면_503을_반환하고_프로필은_변경되지_않는다() throws Exception {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("profile-moderation-error@example.com")
        );

        College college = collegeRepository.save(
                new College("모더레이션장애테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "모더레이션장애테스트학과")
        );

        when(introductionModerator.moderate("검사할 자기소개"))
                .thenThrow(new IntroductionModerationUnavailableException());

        String requestBody = """
            {
              "name": "홍길동",
              "grade": 3,
              "majorId": "%s",
              "gpaBand": "GTE_3_5",
              "introduction": "검사할 자기소개"
            }
            """.formatted(major.getId());

        // when & then
        mockMvc.perform(
                        put("/api/v1/users/me/profile")
                                .with(jwt().jwt(jwt -> jwt
                                        .subject(user.getId().toString())
                                        .claim("role", "USER")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code")
                        .value("CONTENT_MODERATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.fieldErrors").isArray())
                .andExpect(jsonPath("$.error.fieldErrors").isEmpty());

        // DB가 변경되지 않았는지 확인
        AppUser savedUser = appUserRepository.findById(user.getId())
                .orElseThrow();

        assertThat(savedUser.getName()).isNull();
        assertThat(savedUser.getGrade()).isNull();
        assertThat(savedUser.getMajorDepartment()).isNull();
        assertThat(savedUser.getGpaBand()).isNull();
        assertThat(savedUser.getIntroduction()).isEmpty();
        assertThat(savedUser.getProfileUpdatedAt()).isNull();
    }
}
