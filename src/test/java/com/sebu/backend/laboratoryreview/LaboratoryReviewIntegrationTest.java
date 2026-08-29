package com.sebu.backend.laboratoryreview;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratoryreview.domain.Atmosphere;
import com.sebu.backend.laboratoryreview.domain.Compensation;
import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import com.sebu.backend.laboratoryreview.domain.PaperOpportunity;
import com.sebu.backend.laboratoryreview.domain.ParticipationTerm;
import com.sebu.backend.laboratoryreview.domain.ResearchIntensity;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewUpdateRequest;
import com.sebu.backend.laboratoryreview.exception.LaboratoryReviewAlreadyExistsException;
import com.sebu.backend.laboratoryreview.exception.LaboratoryReviewForbiddenException;
import com.sebu.backend.laboratoryreview.repository.LaboratoryReviewRepository;
import com.sebu.backend.laboratoryreview.service.LaboratoryReviewService;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.user.domain.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void createsLaboratoryReview() {
        // given
        TestFixture fixture = createFixture();

        LaboratoryReviewCreateRequest request = createRequest();

        // when
        var response = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                request
        );

        // then
        assertThat(response.reviewId()).isNotNull();

        LaboratoryReview saved = laboratoryReviewRepository
                .findById(response.reviewId())
                .orElseThrow();

        assertThat(saved.getOverallRating()).isEqualTo(5);
        assertThat(saved.getResearchIntensity())
                .isEqualTo(ResearchIntensity.LOW);
        assertThat(saved.getCompensation())
                .isEqualTo(Compensation.SUFFICIENT);
        assertThat(saved.getPaperOpportunity())
                .isEqualTo(PaperOpportunity.AVERAGE);
        assertThat(saved.getAtmosphere())
                .isEqualTo(Atmosphere.COLLABORATIVE);
        assertThat(saved.getParticipationYear()).isEqualTo(2026);
        assertThat(saved.getParticipationTerm())
                .isEqualTo(ParticipationTerm.FIRST_SEMESTER);
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void rejectsDuplicateActiveReview() {
        // given
        TestFixture fixture = createFixture();

        LaboratoryReviewCreateRequest request = createRequest();

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
                .isInstanceOf(
                        LaboratoryReviewAlreadyExistsException.class
                )
                .hasMessage(
                        "LABORATORY_REVIEW_ALREADY_EXISTS"
                );
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

        var review = response.reviews().get(0);

        assertThat(review.overallRating()).isEqualTo(5);
        assertThat(review.researchIntensity()).isEqualTo("LOW");
        assertThat(review.compensation()).isEqualTo("SUFFICIENT");
        assertThat(review.mine()).isTrue();
    }

    @Test
    void returnsMineFalseForAnotherUser() {
        // given
        TestFixture fixture = createFixture();

        laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        );

        AppUser anotherUser =
                new AppUser("another-reviewer@test.com");

        entityManager.persist(anotherUser);
        entityManager.flush();

        // when
        var response = laboratoryReviewService.getReviews(
                fixture.laboratoryId(),
                anotherUser.getId(),
                0,
                20
        );

        // then
        assertThat(response.reviews()).hasSize(1);
        assertThat(response.reviews().get(0).mine()).isFalse();
    }

    @Test
    void returnsMyReview() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        ).reviewId();

        // when
        var response = laboratoryReviewService.getMyReview(
                fixture.laboratoryId(),
                fixture.userId()
        );

        // then
        assertThat(response.review().id()).isEqualTo(reviewId);
        assertThat(response.review().overallRating()).isEqualTo(5);
        assertThat(response.review().participationYear()).isEqualTo(2026);
        assertThat(response.review().participationTerm())
                .isEqualTo("FIRST_SEMESTER");
    }

    @Test
    void updatesOwnReview() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        ).reviewId();

        LaboratoryReviewUpdateRequest request =
                new LaboratoryReviewUpdateRequest(
                        4,
                        ResearchIntensity.MEDIUM,
                        Compensation.SUFFICIENT,
                        PaperOpportunity.MANY,
                        Atmosphere.NORMAL,
                        "실제 참여 경험을 반영해서 충분한 길이로 후기 내용을 수정했습니다.",
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

        entityManager.flush();
        entityManager.clear();

        // then
        LaboratoryReview updated = laboratoryReviewRepository
                .findById(reviewId)
                .orElseThrow();

        assertThat(updated.getOverallRating()).isEqualTo(4);
        assertThat(updated.getResearchIntensity())
                .isEqualTo(ResearchIntensity.MEDIUM);
        assertThat(updated.getPaperOpportunity())
                .isEqualTo(PaperOpportunity.MANY);
        assertThat(updated.getAtmosphere())
                .isEqualTo(Atmosphere.NORMAL);
    }

    @Test
    void rejectsUpdateByAnotherUser() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        ).reviewId();

        AppUser anotherUser =
                new AppUser("another-user@test.com");

        entityManager.persist(anotherUser);
        entityManager.flush();

        LaboratoryReviewUpdateRequest request =
                new LaboratoryReviewUpdateRequest(
                        4,
                        ResearchIntensity.MEDIUM,
                        Compensation.SUFFICIENT,
                        PaperOpportunity.MANY,
                        Atmosphere.NORMAL,
                        "다른 사용자가 임의로 수정하려고 하는 충분한 길이의 후기입니다.",
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
                .isInstanceOf(
                        LaboratoryReviewForbiddenException.class
                )
                .hasMessage(
                        "LABORATORY_REVIEW_FORBIDDEN"
                );
    }

    @Test
    void rejectsDeleteByAnotherUser() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        ).reviewId();

        AppUser anotherUser =
                new AppUser("delete-attacker@test.com");

        entityManager.persist(anotherUser);
        entityManager.flush();

        // when & then
        assertThatThrownBy(() ->
                laboratoryReviewService.deleteReview(
                        fixture.laboratoryId(),
                        reviewId,
                        anotherUser.getId()
                )
        )
                .isInstanceOf(
                        LaboratoryReviewForbiddenException.class
                )
                .hasMessage(
                        "LABORATORY_REVIEW_FORBIDDEN"
                );
    }

    @Test
    void deletesReviewAndExcludesItFromListAndSummary() {
        // given
        TestFixture fixture = createFixture();

        Long reviewId = laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        ).reviewId();

        // when
        laboratoryReviewService.deleteReview(
                fixture.laboratoryId(),
                reviewId,
                fixture.userId()
        );

        entityManager.flush();

        var reviews = laboratoryReviewService.getReviews(
                fixture.laboratoryId(),
                fixture.userId(),
                0,
                20
        );

        var summary = laboratoryReviewService.getReviewSummary(
                fixture.laboratoryId()
        );

        // then
        assertThat(reviews.totalElements()).isZero();
        assertThat(reviews.reviews()).isEmpty();

        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.averageRating()).isNull();

        assertThat(summary.ratingDistribution())
                .allSatisfy(distribution -> {
                    assertThat(distribution.count()).isZero();
                    assertThat(distribution.percentage()).isZero();
                });
    }

    @Test
    void allowsNewReviewAfterPreviousReviewWasDeleted() {
        // given
        TestFixture fixture = createFixture();

        Long firstReviewId =
                laboratoryReviewService.createReview(
                        fixture.laboratoryId(),
                        fixture.userId(),
                        createRequest()
                ).reviewId();

        laboratoryReviewService.deleteReview(
                fixture.laboratoryId(),
                firstReviewId,
                fixture.userId()
        );

        entityManager.flush();

        // when
        var secondResponse =
                laboratoryReviewService.createReview(
                        fixture.laboratoryId(),
                        fixture.userId(),
                        createRequest()
                );

        // then
        assertThat(secondResponse.reviewId()).isNotNull();
        assertThat(secondResponse.reviewId())
                .isNotEqualTo(firstReviewId);
    }

    @Test
    void calculatesReviewSummary() {
        // given
        TestFixture fixture = createFixture();

        laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                fixture.userId(),
                createRequest()
        );

        AppUser secondUser =
                new AppUser("second-reviewer@test.com");

        entityManager.persist(secondUser);
        entityManager.flush();

        LaboratoryReviewCreateRequest secondRequest =
                new LaboratoryReviewCreateRequest(
                        3,
                        ResearchIntensity.HIGH,
                        Compensation.NONE,
                        PaperOpportunity.MANY,
                        Atmosphere.COMPETITIVE,
                        "두 번째 사용자가 작성한 충분한 길이의 연구실 후기 내용입니다.",
                        2026,
                        ParticipationTerm.SECOND_SEMESTER
                );

        laboratoryReviewService.createReview(
                fixture.laboratoryId(),
                secondUser.getId(),
                secondRequest
        );

        // when
        var summary =
                laboratoryReviewService.getReviewSummary(
                        fixture.laboratoryId()
                );

        // then
        assertThat(summary.reviewCount()).isEqualTo(2);
        assertThat(summary.averageRating()).isEqualTo(4.0);

        var fiveStar = summary.ratingDistribution()
                .stream()
                .filter(distribution ->
                        distribution.rating() == 5
                )
                .findFirst()
                .orElseThrow();

        var threeStar = summary.ratingDistribution()
                .stream()
                .filter(distribution ->
                        distribution.rating() == 3
                )
                .findFirst()
                .orElseThrow();

        assertThat(fiveStar.count()).isEqualTo(1);
        assertThat(fiveStar.percentage()).isEqualTo(50.0);

        assertThat(threeStar.count()).isEqualTo(1);
        assertThat(threeStar.percentage()).isEqualTo(50.0);
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
        College college =
                new College("테스트 단과대학");

        entityManager.persist(college);

        Department department =
                new Department(
                        college,
                        "테스트학과"
                );

        entityManager.persist(department);

        Professor professor =
                new Professor(
                        department,
                        "김교수",
                        "professor@test.com"
                );

        entityManager.persist(professor);

        Laboratory laboratory =
                new Laboratory(
                        professor,
                        department,
                        "테스트 연구실",
                        "https://example.com",
                        RecruitmentStatus.RECRUITING
                );

        entityManager.persist(laboratory);

        AppUser user =
                new AppUser("reviewer@test.com");

        entityManager.persist(user);

        entityManager.flush();

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
