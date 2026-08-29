package com.sebu.backend.laboratory;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratoryreview.domain.Atmosphere;
import com.sebu.backend.laboratoryreview.domain.Compensation;
import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import com.sebu.backend.laboratoryreview.domain.PaperOpportunity;
import com.sebu.backend.laboratoryreview.domain.ParticipationTerm;
import com.sebu.backend.laboratoryreview.domain.ResearchIntensity;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.user.domain.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LaboratoryRatingQueryIntegrationTest {

    @Autowired
    LaboratoryRepository laboratoryRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void returnsNullAverageAndZeroCountWhenLaboratoryHasNoReviews() {
        // given
        TestLaboratory fixture = createLaboratory(
                "후기 없는 연구실",
                "no-review-professor@test.com"
        );

        // when
        var result = laboratoryRepository.findAllSummariesByRating(
                null,
                PageRequest.of(0, 20)
        );

        var laboratory = result.getContent()
                .stream()
                .filter(summary ->
                        summary.getId().equals(fixture.laboratoryId())
                )
                .findFirst()
                .orElseThrow();

        // then
        assertThat(laboratory.getAverageRating()).isNull();
        assertThat(laboratory.getReviewCount()).isZero();
    }

    @Test
    void ordersLaboratoriesByAverageRatingDescending() {
        // given
        TestLaboratory high = createLaboratory(
                "평점 높은 연구실",
                "high-rating-professor@test.com"
        );

        TestLaboratory low = createLaboratory(
                "평점 낮은 연구실",
                "low-rating-professor@test.com"
        );

        createReview(
                high,
                "high-user@test.com",
                5
        );

        createReview(
                low,
                "low-user@test.com",
                3
        );

        entityManager.flush();

        // when
        var result = laboratoryRepository.findAllSummariesByRating(
                null,
                PageRequest.of(0, 20)
        );

        var ids = result.getContent()
                .stream()
                .map(summary -> summary.getId())
                .toList();

        // then
        assertThat(ids.indexOf(high.laboratoryId()))
                .isLessThan(ids.indexOf(low.laboratoryId()));
    }

    @Test
    void ordersByReviewCountWhenAverageRatingIsSame() {
        // given
        TestLaboratory manyReviews = createLaboratory(
                "후기 많은 연구실",
                "many-review-professor@test.com"
        );

        TestLaboratory fewReviews = createLaboratory(
                "후기 적은 연구실",
                "few-review-professor@test.com"
        );

        createReview(
                manyReviews,
                "many-user-1@test.com",
                5
        );

        createReview(
                manyReviews,
                "many-user-2@test.com",
                3
        );

        createReview(
                fewReviews,
                "few-user@test.com",
                4
        );

        entityManager.flush();

        // 둘 다 평균 4.0
        // manyReviews = 2개
        // fewReviews = 1개

        // when
        var result = laboratoryRepository.findAllSummariesByRating(
                null,
                PageRequest.of(0, 20)
        );

        var ids = result.getContent()
                .stream()
                .map(summary -> summary.getId())
                .toList();

        // then
        assertThat(ids.indexOf(manyReviews.laboratoryId()))
                .isLessThan(ids.indexOf(fewReviews.laboratoryId()));
    }

    @Test
    void ordersByIdDescendingWhenRatingAndReviewCountAreSame() {
        // given
        TestLaboratory first = createLaboratory(
                "먼저 생성된 연구실",
                "first-professor@test.com"
        );

        TestLaboratory second = createLaboratory(
                "나중 생성된 연구실",
                "second-professor@test.com"
        );

        createReview(
                first,
                "first-user@test.com",
                4
        );

        createReview(
                second,
                "second-user@test.com",
                4
        );

        entityManager.flush();

        // when
        var result = laboratoryRepository.findAllSummariesByRating(
                null,
                PageRequest.of(0, 20)
        );

        var ids = result.getContent()
                .stream()
                .map(summary -> summary.getId())
                .toList();

        // then
        assertThat(second.laboratoryId())
                .isGreaterThan(first.laboratoryId());

        assertThat(ids.indexOf(second.laboratoryId()))
                .isLessThan(ids.indexOf(first.laboratoryId()));
    }

    @Test
    void putsLaboratoriesWithoutReviewsAtTheEnd() {
        // given
        TestLaboratory reviewed = createLaboratory(
                "후기 있는 연구실",
                "reviewed-professor@test.com"
        );

        TestLaboratory noReview = createLaboratory(
                "후기 없는 연구실",
                "empty-professor@test.com"
        );

        createReview(
                reviewed,
                "reviewed-user@test.com",
                1
        );

        entityManager.flush();

        // when
        var result = laboratoryRepository.findAllSummariesByRating(
                null,
                PageRequest.of(0, 20)
        );

        var ids = result.getContent()
                .stream()
                .map(summary -> summary.getId())
                .toList();

        // then
        assertThat(ids.indexOf(reviewed.laboratoryId()))
                .isLessThan(ids.indexOf(noReview.laboratoryId()));
    }

    private TestLaboratory createLaboratory(
            String laboratoryName,
            String professorEmail
    ) {
        College college = new College(
                "단과대-" + laboratoryName
        );
        entityManager.persist(college);

        Department department = new Department(
                college,
                "학과-" + laboratoryName
        );
        entityManager.persist(department);

        Professor professor = new Professor(
                department,
                "교수-" + laboratoryName,
                professorEmail
        );
        entityManager.persist(professor);

        Laboratory laboratory = new Laboratory(
                professor,
                department,
                laboratoryName,
                "https://example.com/" + laboratoryName.hashCode(),
                RecruitmentStatus.RECRUITING
        );
        entityManager.persist(laboratory);

        entityManager.flush();

        return new TestLaboratory(
                laboratory,
                laboratory.getId()
        );
    }

    private void createReview(
            TestLaboratory fixture,
            String email,
            int rating
    ) {
        AppUser user = new AppUser(email);
        entityManager.persist(user);

        LaboratoryReview review = new LaboratoryReview(
                fixture.laboratory(),
                user,
                rating,
                ResearchIntensity.MEDIUM,
                Compensation.SUFFICIENT,
                PaperOpportunity.AVERAGE,
                Atmosphere.NORMAL,
                "평점 정렬 테스트를 위해 충분한 길이로 작성한 연구실 후기 내용입니다.",
                2026,
                ParticipationTerm.FIRST_SEMESTER
        );

        entityManager.persist(review);
    }

    private record TestLaboratory(
            Laboratory laboratory,
            Long laboratoryId
    ) {
    }
}
