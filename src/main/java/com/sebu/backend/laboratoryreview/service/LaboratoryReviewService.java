package com.sebu.backend.laboratoryreview.service;

import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.exception.LaboratoryNotFoundException;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateRequest;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewCreateResponse;
import com.sebu.backend.laboratoryreview.dto.LaboratoryReviewListResponse;
import com.sebu.backend.laboratoryreview.exception.InvalidReviewPageException;
import com.sebu.backend.laboratoryreview.exception.InvalidReviewSizeException;
import com.sebu.backend.laboratoryreview.exception.LaboratoryReviewAlreadyExistsException;
import com.sebu.backend.laboratoryreview.repository.LaboratoryReviewRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.exception.UserNotFoundException;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Laboratory laboratory = laboratoryRepository
                .findById(laboratoryId)
                .filter(lab -> !lab.isDeleted())
                .orElseThrow(LaboratoryNotFoundException::new);

        AppUser author = appUserRepository
                .findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(UserNotFoundException::new);

        boolean alreadyExists =
                laboratoryReviewRepository
                        .existsByLaboratoryIdAndAuthorId(
                                laboratoryId,
                                userId
                        );

        if (alreadyExists) {
            throw new LaboratoryReviewAlreadyExistsException();
        }

        LaboratoryReview review = new LaboratoryReview(
                laboratory,
                author,
                request.category(),
                request.researchIntensity(),
                request.compensation(),
                request.atmosphere(),
                request.tags(),
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
        validatePagination(page, size);

        Laboratory laboratory = laboratoryRepository
                .findById(laboratoryId)
                .filter(lab -> !lab.isDeleted())
                .orElseThrow(LaboratoryNotFoundException::new);

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
                                laboratoryId,
                                pageable
                        );

        List<LaboratoryReviewListResponse.ReviewItem> reviews =
                reviewPage.getContent()
                        .stream()
                        .map(
                                LaboratoryReviewListResponse
                                        .ReviewItem::from
                        )
                        .toList();

        boolean reviewedByMe =
                currentUserId != null
                        && laboratoryReviewRepository
                        .existsByLaboratoryIdAndAuthorIdAndDeletedAtIsNull(
                                laboratoryId,
                                currentUserId
                        );

        return new LaboratoryReviewListResponse(
                LaboratoryReviewListResponse
                        .LaboratoryInfo
                        .from(laboratory),
                reviewedByMe,
                reviews,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.hasNext()
        );
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new InvalidReviewPageException();
        }

        if (size < 1 || size > 50) {
            throw new InvalidReviewSizeException();
        }
    }
}
