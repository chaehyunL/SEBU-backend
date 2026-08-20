package com.sebu.backend.bookmark.repository;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    long countByUser_Id(Long userId);

    List<Bookmark> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByLaboratory_Id(Long laboratoryId);

    @Query("""
            select b
            from Bookmark b
            where b.user.id = :userId
              and (
                    :cursorCreatedAt is null
                    or b.createdAt < :cursorCreatedAt
                    or (
                        b.createdAt = :cursorCreatedAt
                        and b.laboratory.id < :cursorLaboratoryId
                    )
              )
            order by b.createdAt desc, b.laboratory.id desc
            """)
    List<Bookmark> findBookmarkedLaboratories(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorLaboratoryId") Long cursorLaboratoryId,
            Pageable pageable
    );
}
