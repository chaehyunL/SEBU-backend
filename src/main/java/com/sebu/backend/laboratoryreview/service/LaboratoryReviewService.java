package com.sebu.backend.laboratoryreview.service;

import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratoryreview.domain.Atmosphere;
import com.sebu.backend.laboratoryreview.domain.Compensation;
import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import com.sebu.backend.laboratoryreview.domain.PaperOpportunity;
import com.sebu.backend.laboratoryreview.domain.ResearchIntensity;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewDeleteResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewListResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewMeResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewSummaryResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewUpdateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewUpdateResponse;
import com.sebu.backend.laboratoryreview.repository.LaboratoryReviewRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class LaboratoryReviewService {

    private final LaboratoryReviewRepository laboratoryReviewRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final AppUserRepository appUserRepository;

    /*
     * 후기 작성
     */
    @Transactional
    public LaboratoryReviewCreateResponse createReview(
            Long laboratoryId,
            Long userId,
            LaboratoryReviewCreateRequest request
    ) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .filter(lab -> !lab.isDeleted())
                .orElseThrow(() ->
                        new IllegalArgumentException("LABORATORY_NOT_FOUND")
                );

        AppUser author = appUserRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() ->
                        new IllegalArgumentException("USER_NOT_FOUND")
                );

        boolean alreadyExists =
                laboratoryReviewRepository
                        .existsByLaboratoryIdAndAuthorIdAndDeletedAtIsNull(
                                laboratoryId,
                                userId
                        );

        if (alreadyExists) {
            throw new IllegalStateException(
                    "LABORATORY_REVIEW_ALREADY_EXISTS"
            );
        }

        LaboratoryReview review = new LaboratoryReview(
                laboratory,
                author,
                request.overallRating(),
                request.researchIntensity(),
                request.compensation(),
                request.paperOpportunity(),
                request.atmosphere(),
                request.content(),
                request.participationYear(),
                request.participationTerm()
        );

        LaboratoryReview saved =
                laboratoryReviewRepository.save(review);

        return new LaboratoryReviewCreateResponse(
                saved.getId()
        );
    }

    /*
     * 특정 연구실 후기 목록 조회
     */
    @Transactional(readOnly = true)
    public LaboratoryReviewListResponse getReviews(
            Long laboratoryId,
            Long currentUserId,
            int page,
            int size
    ) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .filter(lab -> !lab.isDeleted())
                .orElseThrow(() ->
                        new IllegalArgumentException("LABORATORY_NOT_FOUND")
                );

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<LaboratoryReview> reviewPage =
                laboratoryReviewRepository
                        .findByLaboratoryIdAndDeletedAtIsNull(
                                laboratory.getId(),
                                pageable
                        );

        var reviews = reviewPage.getContent()
                .stream()
                .map(review ->
                        LaboratoryReviewListResponse.ReviewItem.from(
                                review,
                                currentUserId
                        )
                )
                .toList();

        return new LaboratoryReviewListResponse(
                reviews,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.hasNext()
        );
    }

    /*
     * 현재 로그인 사용자의 해당 연구실 후기 조회
     */
    @Transactional(readOnly = true)
    public LaboratoryReviewMeResponse getMyReview(
            Long laboratoryId,
            Long userId
    ) {
        laboratoryRepository.findById(laboratoryId)
                .filter(lab -> !lab.isDeleted())
                .orElseThrow(() ->
                        new IllegalArgumentException("LABORATORY_NOT_FOUND")
                );

        LaboratoryReview review =
                laboratoryReviewRepository
                        .findByLaboratoryIdAndAuthorIdAndDeletedAtIsNull(
                                laboratoryId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "LABORATORY_REVIEW_NOT_FOUND"
                                )
                        );

        return LaboratoryReviewMeResponse.from(review);
    }

    /*
     * 후기 수정
     */
    @Transactional
    public LaboratoryReviewUpdateResponse updateReview(
            Long laboratoryId,
            Long reviewId,
            Long userId,
            LaboratoryReviewUpdateRequest request
    ) {
        LaboratoryReview review = findActiveReview(
                laboratoryId,
                reviewId
        );

        if (!review.isWrittenBy(userId)) {
            throw new IllegalStateException(
                    "LABORATORY_REVIEW_FORBIDDEN"
            );
        }

        review.update(
                request.overallRating(),
                request.researchIntensity(),
                request.compensation(),
                request.paperOpportunity(),
                request.atmosphere(),
                request.content(),
                request.participationYear(),
                request.participationTerm()
        );

        return new LaboratoryReviewUpdateResponse(
                review.getId(),
                review.getUpdatedAt()
        );
    }

    /*
     * 후기 삭제
     */
    @Transactional
    public LaboratoryReviewDeleteResponse deleteReview(
            Long laboratoryId,
            Long reviewId,
            Long userId
    ) {
        LaboratoryReview review = findActiveReview(
                laboratoryId,
                reviewId
        );

        if (!review.isWrittenBy(userId)) {
            throw new IllegalStateException(
                    "LABORATORY_REVIEW_FORBIDDEN"
            );
        }

        review.softDelete();

        return new LaboratoryReviewDeleteResponse(
                review.getId()
        );
    }

    /*
     * 연구실 후기 평가 요약 조회
     */
    @Transactional(readOnly = true)
    public LaboratoryReviewSummaryResponse getReviewSummary(
            Long laboratoryId
    ) {
        Laboratory laboratory = laboratoryRepository.findById(laboratoryId)
                .filter(lab -> !lab.isDeleted())
                .orElseThrow(() ->
                        new IllegalArgumentException("LABORATORY_NOT_FOUND")
                );

        List<LaboratoryReview> reviews =
                laboratoryReviewRepository
                        .findAllByLaboratoryIdAndDeletedAtIsNull(
                                laboratoryId
                        );

        long reviewCount = reviews.size();

        Double averageRating = reviewCount == 0
                ? null
                : reviews.stream()
                .mapToInt(LaboratoryReview::getOverallRating)
                .average()
                .orElse(0.0);

        List<LaboratoryReviewSummaryResponse.RatingDistribution>
                ratingDistribution =
                createRatingDistribution(reviews);

        LaboratoryReviewSummaryResponse.EvaluationDistributions
                evaluationDistributions =
                new LaboratoryReviewSummaryResponse.EvaluationDistributions(
                        createEnumDistribution(
                                ResearchIntensity.values(),
                                reviews,
                                LaboratoryReview::getResearchIntensity
                        ),
                        createEnumDistribution(
                                Compensation.values(),
                                reviews,
                                LaboratoryReview::getCompensation
                        ),
                        createEnumDistribution(
                                PaperOpportunity.values(),
                                reviews,
                                LaboratoryReview::getPaperOpportunity
                        ),
                        createEnumDistribution(
                                Atmosphere.values(),
                                reviews,
                                LaboratoryReview::getAtmosphere
                        )
                );

        return new LaboratoryReviewSummaryResponse(
                createLaboratoryInfo(laboratory),
                averageRating,
                reviewCount,
                ratingDistribution,
                evaluationDistributions
        );
    }

    /*
     * 삭제되지 않은 특정 후기 조회
     */
    private LaboratoryReview findActiveReview(
            Long laboratoryId,
            Long reviewId
    ) {
        LaboratoryReview review =
                laboratoryReviewRepository
                        .findByIdAndDeletedAtIsNull(reviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "LABORATORY_REVIEW_NOT_FOUND"
                                )
                        );

        if (!review.getLaboratory().getId().equals(laboratoryId)) {
            throw new IllegalArgumentException(
                    "LABORATORY_REVIEW_NOT_FOUND"
            );
        }

        return review;
    }

    /*
     * 별점 5~1점 분포 계산
     */
    private List<LaboratoryReviewSummaryResponse.RatingDistribution>
    createRatingDistribution(
            List<LaboratoryReview> reviews
    ) {
        long total = reviews.size();

        return List.of(5, 4, 3, 2, 1)
                .stream()
                .map(rating -> {
                    long count = reviews.stream()
                            .filter(review ->
                                    review.getOverallRating() == rating
                            )
                            .count();

                    return new LaboratoryReviewSummaryResponse
                            .RatingDistribution(
                            rating,
                            count,
                            percentage(count, total)
                    );
                })
                .toList();
    }

    /*
     * 연구 강도 / 인건비 / 논문 기회 / 분위기 분포 계산
     */
    private <E extends Enum<E>>
    List<LaboratoryReviewSummaryResponse.EvaluationDistribution>
    createEnumDistribution(
            E[] values,
            List<LaboratoryReview> reviews,
            Function<LaboratoryReview, E> extractor
    ) {
        long total = reviews.size();

        return Arrays.stream(values)
                .map(value -> {
                    long count = reviews.stream()
                            .filter(review ->
                                    extractor.apply(review) == value
                            )
                            .count();

                    return new LaboratoryReviewSummaryResponse
                            .EvaluationDistribution(
                            value.name(),
                            count,
                            percentage(count, total)
                    );
                })
                .toList();
    }

    /*
     * 비율 계산
     */
    private double percentage(
            long count,
            long total
    ) {
        if (total == 0) {
            return 0.0;
        }

        return (count * 100.0) / total;
    }

    /*
     * 평가 요약에 포함할 연구실 정보 생성
     */
    private LaboratoryReviewSummaryResponse.LaboratoryInfo
    createLaboratoryInfo(
            Laboratory laboratory
    ) {
        var professor = laboratory.getProfessor();
        var department = laboratory.getDepartment();
        var college = department.getCollege();

        return new LaboratoryReviewSummaryResponse.LaboratoryInfo(
                laboratory.getId(),
                laboratory.getName(),
                new LaboratoryReviewSummaryResponse.ProfessorInfo(
                        professor.getId(),
                        professor.getName()
                ),
                new LaboratoryReviewSummaryResponse.CollegeInfo(
                        college.getId(),
                        college.getName()
                ),
                new LaboratoryReviewSummaryResponse.DepartmentInfo(
                        department.getId(),
                        department.getName()
                )
        );
    }
}
