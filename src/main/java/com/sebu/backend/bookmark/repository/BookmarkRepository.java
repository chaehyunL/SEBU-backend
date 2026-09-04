package com.sebu.backend.bookmark.repository;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    long countByUser_Id(Long userId);

    List<Bookmark> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select count(b)
            from Bookmark b
            where b.laboratory.id = :laboratoryId
              and b.user.deletedAt is null
            """)
    long countActiveByLaboratoryId(
            @Param("laboratoryId") Long laboratoryId
    );

    @Query("""
            select b
            from Bookmark b
            where b.user.id = :userId
            order by b.createdAt desc, b.laboratory.id desc
            """)
    List<Bookmark> findBookmarkedLaboratories(
            @Param("userId") Long userId
    );
}
