package com.sebu.backend.laboratoryreview.domain;

import com.sebu.backend.global.domain.BaseTimeEntity;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.user.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Objects;

@Getter
@Entity
@Table(name = "laboratory_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LaboratoryReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(name = "overall_rating", nullable = false)
    private int overallRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "research_intensity", nullable = false, length = 30)
    private ResearchIntensity researchIntensity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Compensation compensation;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_opportunity", nullable = false, length = 30)
    private PaperOpportunity paperOpportunity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Atmosphere atmosphere;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "participation_year", nullable = false)
    private int participationYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_term", nullable = false, length = 30)
    private ParticipationTerm participationTerm;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public LaboratoryReview(
            Laboratory laboratory,
            AppUser author,
            int overallRating,
            ResearchIntensity researchIntensity,
            Compensation compensation,
            PaperOpportunity paperOpportunity,
            Atmosphere atmosphere,
            String content,
            int participationYear,
            ParticipationTerm participationTerm
    ) {
        this.laboratory = Objects.requireNonNull(
                laboratory,
                "LABORATORY_REQUIRED"
        );
        this.author = Objects.requireNonNull(
                author,
                "AUTHOR_REQUIRED"
        );

        applyReview(
                overallRating,
                researchIntensity,
                compensation,
                paperOpportunity,
                atmosphere,
                content,
                participationYear,
                participationTerm
        );
    }

    public void update(
            int overallRating,
            ResearchIntensity researchIntensity,
            Compensation compensation,
            PaperOpportunity paperOpportunity,
            Atmosphere atmosphere,
            String content,
            int participationYear,
            ParticipationTerm participationTerm
    ) {
        if (isDeleted()) {
            throw new IllegalStateException(
                    "DELETED_LABORATORY_REVIEW_CANNOT_BE_UPDATED"
            );
        }

        applyReview(
                overallRating,
                researchIntensity,
                compensation,
                paperOpportunity,
                atmosphere,
                content,
                participationYear,
                participationTerm
        );
    }

    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isWrittenBy(Long userId) {
        return Objects.equals(author.getId(), userId);
    }

    private void applyReview(
            int overallRating,
            ResearchIntensity researchIntensity,
            Compensation compensation,
            PaperOpportunity paperOpportunity,
            Atmosphere atmosphere,
            String content,
            int participationYear,
            ParticipationTerm participationTerm
    ) {
        validateOverallRating(overallRating);
        validateParticipationYear(participationYear);

        this.overallRating = overallRating;
        this.researchIntensity = Objects.requireNonNull(
                researchIntensity,
                "RESEARCH_INTENSITY_REQUIRED"
        );
        this.compensation = Objects.requireNonNull(
                compensation,
                "COMPENSATION_REQUIRED"
        );
        this.paperOpportunity = Objects.requireNonNull(
                paperOpportunity,
                "PAPER_OPPORTUNITY_REQUIRED"
        );
        this.atmosphere = Objects.requireNonNull(
                atmosphere,
                "ATMOSPHERE_REQUIRED"
        );
        this.content = normalizeContent(content);
        this.participationYear = participationYear;
        this.participationTerm = Objects.requireNonNull(
                participationTerm,
                "PARTICIPATION_TERM_REQUIRED"
        );
    }

    private void validateOverallRating(int overallRating) {
        if (overallRating < 1 || overallRating > 5) {
            throw new IllegalArgumentException(
                    "INVALID_REVIEW_RATING"
            );
        }
    }

    private void validateParticipationYear(int participationYear) {
        int currentYear = Year.now().getValue();

        if (participationYear < 2000 || participationYear > currentYear) {
            throw new IllegalArgumentException(
                    "INVALID_PARTICIPATION_YEAR"
            );
        }
    }

    private String normalizeContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException(
                    "REVIEW_CONTENT_REQUIRED"
            );
        }

        String normalized = content.trim();

        if (normalized.length() < 20 || normalized.length() > 2000) {
            throw new IllegalArgumentException(
                    "INVALID_REVIEW_CONTENT"
            );
        }

        return normalized;
    }
}
