package com.sebu.backend.laboratoryreview.repository;

import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratoryReviewRepository
        extends JpaRepository<LaboratoryReview, Long> {

    boolean existsByLaboratoryIdAndAuthorIdAndDeletedAtIsNull(
            Long laboratoryId,
            Long authorId
    );

    Optional<LaboratoryReview>
    findByLaboratoryIdAndAuthorIdAndDeletedAtIsNull(
            Long laboratoryId,
            Long authorId
    );

    Optional<LaboratoryReview>
    findByIdAndLaboratoryIdAndDeletedAtIsNull(
            Long reviewId,
            Long laboratoryId
    );

    Page<LaboratoryReview>
    findByLaboratoryIdAndDeletedAtIsNull(
            Long laboratoryId,
            Pageable pageable
    );

    List<LaboratoryReview> findAllByLaboratoryIdAndDeletedAtIsNull(
            Long laboratoryId
    );
}
