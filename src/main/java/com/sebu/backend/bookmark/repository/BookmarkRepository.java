package com.sebu.backend.bookmark.repository;
import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    long countByUser_Id(Long userId);

    List<Bookmark> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByLaboratory_Id(Long laboratoryId);
}
