package com.sebu.backend.mypage.service;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.mypage.dto.ProfileResponse;
import com.sebu.backend.mypage.dto.ProfileUpdateRequest;
import com.sebu.backend.mypage.moderation.IntroductionModerationException;
import com.sebu.backend.mypage.moderation.IntroductionModerator;
import com.sebu.backend.mypage.moderation.ModerationResult;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.domain.GpaBand;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class ProfileServiceTest {

    @Autowired
    ProfileService profileService;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    CollegeRepository collegeRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Test
    void 프로필을_최초_저장할_수_있다() {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("profile-test@example.com")
        );

        College college = collegeRepository.save(
                new College("프로필테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "AI로봇학과")
        );

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        " 홍길동 ",
                        (short) 3,
                        major.getId().toString(),
                        GpaBand.GTE_3_5,
                        "머신러닝에 관심이 있습니다."
                );

        // when
        ProfileResponse response =
                profileService.updateProfile(
                        user.getId(),
                        request
                );

        // then
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.grade()).isEqualTo((short) 3);

        assertThat(response.major().id())
                .isEqualTo(major.getId().toString());

        assertThat(response.major().name())
                .isEqualTo("AI로봇학과");

        assertThat(response.gpaBand())
                .isEqualTo(GpaBand.GTE_3_5);

        assertThat(response.introduction())
                .isEqualTo("머신러닝에 관심이 있습니다.");

        assertThat(response.profileCompleted()).isTrue();
        assertThat(response.profileUpdatedAt()).isNotNull();
    }

    @Test
    void 같은_프로필을_다시_저장하면_profileUpdatedAt은_변경되지_않는다() {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("profile-same@example.com")
        );

        College college = collegeRepository.save(
                new College("프로필동일테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "프로필동일학과")
        );

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "홍길동",
                        (short) 3,
                        major.getId().toString(),
                        GpaBand.GTE_3_5,
                        "머신러닝에 관심이 있습니다."
                );

        ProfileResponse first =
                profileService.updateProfile(
                        user.getId(),
                        request
                );

        LocalDateTime firstUpdatedAt =
                first.profileUpdatedAt();

        // when
        ProfileResponse second =
                profileService.updateProfile(
                        user.getId(),
                        request
                );

        // then
        assertThat(second.profileUpdatedAt())
                .isEqualTo(firstUpdatedAt);
    }

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
    void 자기소개가_정책에_위반되면_프로필은_저장되지_않는다() {
        // given
        AppUser user = appUserRepository.save(
                new AppUser("moderation-fail@example.com")
        );

        College college = collegeRepository.save(
                new College("모더레이션테스트대학")
        );

        Department major = departmentRepository.save(
                new Department(college, "모더레이션테스트학과")
        );

        ProfileUpdateRequest request =
                new ProfileUpdateRequest(
                        "홍길동",
                        (short) 3,
                        major.getId().toString(),
                        GpaBand.GTE_3_5,
                        "차단될 자기소개"
                );

        when(introductionModerator.moderate("차단될 자기소개"))
                .thenReturn(
                        new ModerationResult(
                                false,
                                "v1",
                                "test-provider"
                        )
                );

        // when & then
        assertThatThrownBy(() ->
                profileService.updateProfile(
                        user.getId(),
                        request
                )
        )
                .isInstanceOf(IntroductionModerationException.class);

        // 실제 DB 반영 여부 확인
        AppUser savedUser = appUserRepository.findById(user.getId())
                .orElseThrow();

        assertThat(savedUser.getName()).isNull();
        assertThat(savedUser.getGrade()).isNull();
        assertThat(savedUser.getMajorDepartment()).isNull();
        assertThat(savedUser.getGpaBand()).isNull();
        assertThat(savedUser.getIntroduction()).isEmpty();
        assertThat(savedUser.isProfileCompleted()).isFalse();
        assertThat(savedUser.getProfileUpdatedAt()).isNull();
    }
}
