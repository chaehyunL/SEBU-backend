package com.sebu.backend.laboratoryreview.domain;

import com.sebu.backend.college.domain.College;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.user.domain.AppUser;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LaboratoryReviewTest {

    @Test
    void doesNotExposeMutableTags() {
        // given
        LaboratoryReview review = createReview(
                Set.of(LaboratoryReviewTag.PROJECT_OPPORTUNITY)
        );

        // when & then
        assertThatThrownBy(() -> review.getTags().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(review.getTags())
                .containsExactly(LaboratoryReviewTag.PROJECT_OPPORTUNITY);
    }

    @Test
    void doesNotAllowAddingTagThroughGetter() {
        // given
        LaboratoryReview review = createReview(
                Set.of(LaboratoryReviewTag.PROJECT_OPPORTUNITY)
        );

        // when & then
        assertThatThrownBy(() ->
                review.getTags().add(
                        LaboratoryReviewTag.ACTIVE_FEEDBACK
                )
        ).isInstanceOf(UnsupportedOperationException.class);

        assertThat(review.getTags())
                .containsExactly(LaboratoryReviewTag.PROJECT_OPPORTUNITY);
    }

    @Test
    void defensivelyCopiesTagsPassedToConstructor() {
        // given
        Set<LaboratoryReviewTag> tags = new LinkedHashSet<>();
        tags.add(LaboratoryReviewTag.PROJECT_OPPORTUNITY);

        LaboratoryReview review = createReview(tags);

        // when
        tags.clear();
        tags.add(LaboratoryReviewTag.ACTIVE_FEEDBACK);

        // then
        assertThat(review.getTags())
                .containsExactly(
                        LaboratoryReviewTag.PROJECT_OPPORTUNITY
                );
    }

    @Test
    void replacesTagsThroughDomainMethod() {
        // given
        LaboratoryReview review = createReview(
                Set.of(LaboratoryReviewTag.PROJECT_OPPORTUNITY)
        );

        // when
        review.replaceTags(
                Set.of(
                        LaboratoryReviewTag.ACTIVE_FEEDBACK,
                        LaboratoryReviewTag.PROFESSOR_COMMUNICATION
                )
        );

        // then
        assertThat(review.getTags())
                .containsExactlyInAnyOrder(
                        LaboratoryReviewTag.ACTIVE_FEEDBACK,
                        LaboratoryReviewTag.PROFESSOR_COMMUNICATION
                );
    }

    @Test
    void rejectsNullTagWhenReplacingTags() {
        // given
        LaboratoryReview review = createReview(
                Set.of(LaboratoryReviewTag.PROJECT_OPPORTUNITY)
        );

        Set<LaboratoryReviewTag> invalidTags =
                new LinkedHashSet<>();
        invalidTags.add(LaboratoryReviewTag.ACTIVE_FEEDBACK);
        invalidTags.add(null);

        // when & then
        assertThatThrownBy(() ->
                review.replaceTags(invalidTags)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_LABORATORY_REVIEW_TAG");

        assertThat(review.getTags())
                .containsExactly(
                        LaboratoryReviewTag.PROJECT_OPPORTUNITY
                );
    }

    @Test
    void doesNotAllowReplacingTagsAfterSoftDelete() {
        // given
        LaboratoryReview review = createReview(
                Set.of(LaboratoryReviewTag.PROJECT_OPPORTUNITY)
        );

        review.softDelete();

        // when & then
        assertThatThrownBy(() ->
                review.replaceTags(
                        Set.of(LaboratoryReviewTag.ACTIVE_FEEDBACK)
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "DELETED_LABORATORY_REVIEW_CANNOT_BE_UPDATED"
                );

        assertThat(review.getTags())
                .containsExactly(
                        LaboratoryReviewTag.PROJECT_OPPORTUNITY
                );
    }

    private LaboratoryReview createReview(
            Set<LaboratoryReviewTag> tags
    ) {
        College college =
                new College("테스트 단과대학");

        Department department =
                new Department(
                        college,
                        "테스트학과"
                );

        Professor professor =
                new Professor(
                        department,
                        "김교수",
                        "professor@test.com"
                );

        Laboratory laboratory =
                new Laboratory(
                        professor,
                        department,
                        "테스트 연구실",
                        "https://example.com",
                        RecruitmentStatus.RECRUITING
                );

        AppUser user =
                new AppUser("reviewer@test.com");

        return new LaboratoryReview(
                laboratory,
                user,
                LaboratoryReviewCategory.RESEARCH_ENVIRONMENT,
                ResearchIntensity.MEDIUM,
                Compensation.SMALL_AMOUNT,
                Atmosphere.COOPERATIVE,
                tags,
                "연구실 후기 도메인 객체의 태그 캡슐화를 테스트하기 위한 내용입니다.",
                2026,
                ParticipationTerm.FIRST_SEMESTER
        );
    }
}
