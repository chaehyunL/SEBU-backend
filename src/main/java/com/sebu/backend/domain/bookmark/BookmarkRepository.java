package com.sebu.backend.domain.bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> { }
