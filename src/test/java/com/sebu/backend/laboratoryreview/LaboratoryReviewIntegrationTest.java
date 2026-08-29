package com.sebu.backend.laboratoryreview;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.college.repository.CollegeRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratoryreview.domain.Atmosphere;
import com.sebu.backend.laboratoryreview.domain.Compensation;
import com.sebu.backend.laboratoryreview.domain.PaperOpportunity;
import com.sebu.backend.laboratoryreview.domain.ParticipationTerm;
import com.sebu.backend.laboratoryreview.domain.ResearchIntensity;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewUpdateRequest;
import com.sebu.backend.laboratoryreview.repository.LaboratoryReviewRepository;
import com.sebu.backend.laboratoryreview.service.LaboratoryReviewService;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.professor.repository.ProfessorRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LaboratoryReviewIntegrationTest {

    @Autowired
    LaboratoryReviewService laboratoryReviewService;

    @Autowired
    LaboratoryReviewRepository laboratoryReviewRepository;

    @Autowired
    LaboratoryRepository laboratoryRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    CollegeRepository collegeRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    ProfessorRepository professorRepository;

    @Test
    void createsLaboratoryReview() {
        // given
        TestFixture fixture = createFixture();

        LaboratoryReviewCreateRequest request =
                createRequest();

        // when
        var response = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                request
        );

        // then
        assertThat(response.reviewId()).isNotNull();

        var saved = laboratoryReviewRepository
                .findById(response.reviewId())
                .orElseThrow();

        assertThat(saved.getOverallRating()).isEqualTo(5);
        assertThat(saved.getResearchIntensity())
                .isEqualTo(ResearchIntensity.LOW);
        assertThat(saved.getCompensation())
                .isEqualTo(Compensation.SUFFICIENT);
        assertThat(saved.getAtmosphere())
                .isEqualTo(Atmosphere.COLLABORATIVE);
    }

    @Test
    void rejectsDuplicateActiveReview() {
        // given
        TestFixture fixture = createFixture();

        LaboratoryReviewCreateRequest request =
                createRequest();

        laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                request
        );

        // when & then
        assertThatThrownBy(() ->
                laboratoryReviewService.createReview(
                        fixture.laboratoryId(),
                        fixture.userId(),
                        request
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LABORATORY_REVIEW_ALREADY_EXISTS");
    }

    @Test
    void returnsLaboratoryReviews() {
        // given
        TestFixture fixture = createFixture();

        laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        );

        // when
        var response = laboratoryReviewService.getReviews(
                fixture.laboratoryId(),
                fixture.userId(),
                0,
                20
        );

        // then
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().get(0).mine()).isTrue();
    }

    @Test
    void updatesOwnReview() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService
                .createReview(
                        fixture.laboratoryId(),
                        fixture.userId(),
                        createRequest()
                )
                .reviewId();

        LaboratoryReviewUpdateRequest request =
                new LaboratoryReviewUpdateRequest(
                        4,
                        ResearchIntensity.MEDIUM,
                        Compensation.SUFFICIENT,
                        PaperOpportunity.MANY,
                        Atmosphere.COLLABORATIVE,
                        "수정된 후기 내용이며 최소 글자 수 조건을 만족합니다.",
                        2026,
                        ParticipationTerm.FIRST_SEMESTER
                );

        // when
        laboratoryReviewService.updateReview(
                fixture.laboratoryId(),
                reviewId,
                fixture.userId(),
                request
        );

        // then
        var updated = laboratoryReviewRepository
                .findById(reviewId)
                .orElseThrow();

        assertThat(updated.getOverallRating()).isEqualTo(4);
        assertThat(updated.getResearchIntensity())
                .isEqualTo(ResearchIntensity.MEDIUM);
        assertThat(updated.getPaperOpportunity())
                .isEqualTo(PaperOpportunity.MANY);
    }

    @Test
    void rejectsUpdateByAnotherUser() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService
                .createReview(
                        fixture.laboratoryId(),
                        fixture.userId(),
                        createRequest()
                )
                .reviewId();

        AppUser anotherUser =
                appUserRepository.save(
                        new AppUser("another@example.com")
                );

        LaboratoryReviewUpdateRequest request =
                new LaboratoryReviewUpdateRequest(
                        4,
                        ResearchIntensity.MEDIUM,
                        Compensation.SUFFICIENT,
                        PaperOpportunity.MANY,
                        Atmosphere.NORMAL,
                        "다른 사용자가 수정하려고 시도하는 테스트 후기 내용입니다.",
                        2026,
                        ParticipationTerm.FIRST_SEMESTER
                );

        // when & then
        assertThatThrownBy(() ->
                laboratoryReviewService.updateReview(
                        fixture.laboratoryId(),
                        reviewId,
                        anotherUser.getId(),
                        request
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LABORATORY_REVIEW_FORBIDDEN");
    }

    @Test
    void excludesDeletedReviewFromListAndSummary() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService
                .createReview(
                        fixture.laboratoryId(),
                        fixture.userId(),
                        createRequest()
                )
                .reviewId();

        // when
        laboratoryReviewService.deleteReview(
                fixture.laboratoryId(),
                reviewId,
                fixture.userId()
        );

        var reviews = laboratoryReviewService.getReviews(
                fixture.laboratoryId(),
                fixture.userId(),
                0,
                20
        );

        var summary =
                laboratoryReviewService.getReviewSummary(
                        fixture.laboratoryId()
                );

        // then
        assertThat(reviews.totalElements()).isZero();
        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.averageRating()).isNull();
    }

    private LaboratoryReviewCreateRequest createRequest() {
        return new LaboratoryReviewCreateRequest(
                5,
                ResearchIntensity.LOW,
                Compensation.SUFFICIENT,
                PaperOpportunity.AVERAGE,
                Atmosphere.COLLABORATIVE,
                "프로젝트 참여 기회가 많고 피드백을 자주 받을 수 있었습니다.",
                2026,
                ParticipationTerm.FIRST_SEMESTER
        );
    }

    private TestFixture createFixture() {
        College college = collegeRepository.save(
                new College("테스트 단과대학")
        );

        Department department = departmentRepository.save(
                new Department(
                        college,
                        "테스트학과"
                )
        );

        Professor professor = professorRepository.save(
                new Professor(
                        department,
                        "김교수",
                        "professor@test.com"
                )
        );

        Laboratory laboratory = laboratoryRepository.save(
                new Laboratory(
                        professor,
                        department,
                        "테스트 연구실",
                        "https://example.com",
                        RecruitmentStatus.RECRUITING
                )
        );

        AppUser user = appUserRepository.save(
                new AppUser("reviewer@test.com")
        );

        return new TestFixture(
                laboratory.getId(),
                user.getId()
        );
    }

    private record TestFixture(
            Long laboratoryId,
            Long userId
    ) {
    }
}
