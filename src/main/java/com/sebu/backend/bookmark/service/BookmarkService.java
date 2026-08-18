package com.sebu.backend.bookmark.service;

import com.sebu.backend.bookmark.domain.Bookmark;
import com.sebu.backend.bookmark.domain.BookmarkId;
import com.sebu.backend.bookmark.repository.BookmarkRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final AppUserRepository appUserRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public Bookmark add(Long userId, Long laboratoryId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Laboratory laboratory = laboratoryRepository.findByIdAndDeletedAtIsNull(laboratoryId)
            .orElseThrow(() -> new IllegalArgumentException("Laboratory not found"));
        BookmarkId bookmarkId = new BookmarkId(userId, laboratoryId);

        if (bookmarkRepository.existsById(bookmarkId)) {
            throw new IllegalStateException("BOOKMARK_DUPLICATED");
        }

        return bookmarkRepository.save(new Bookmark(user, laboratory));
    }
}
