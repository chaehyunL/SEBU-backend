package com.sebu.backend.laboratoryreview.service;

import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewListResponse;
import com.sebu.backend.laboratoryreview.repository.LaboratoryReviewRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LaboratoryReviewService {

    private final LaboratoryReviewRepository laboratoryReviewRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final AppUserRepository appUserRepository;

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
}
