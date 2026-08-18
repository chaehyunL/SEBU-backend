package com.sebu.backend.bookmark.repository;
import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> { }
