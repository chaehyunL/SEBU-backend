package com.sebu.backend.laboratoryreview.repository;

import com.sebu.backend.laboratoryreview.domain.LaboratoryReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryReviewRepository
        extends JpaRepository<LaboratoryReview, Long> {

    boolean existsByLaboratoryIdAndAuthorId(
            Long laboratoryId,
            Long authorId
    );

    Page<LaboratoryReview> findByLaboratoryIdAndDeletedAtIsNull(
            Long laboratoryId,
            Pageable pageable
    );

    boolean existsByLaboratoryIdAndAuthorIdAndDeletedAtIsNull(
            Long laboratoryId,
            Long authorId
    );
}
