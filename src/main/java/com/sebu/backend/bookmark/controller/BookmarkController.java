package com.sebu.backend.bookmark.controller;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.bookmark.dto.BookmarkedLaboratoriesResponse;
import com.sebu.backend.bookmark.service.BookmarkService;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/api/v1/users/me/bookmarked-laboratories")
    public ResponseEntity<ApiResponse<BookmarkedLaboratoriesResponse>>
    getBookmarkedLaboratories(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);

        BookmarkedLaboratoriesResponse response =
                bookmarkService.getBookmarkedLaboratories(
                        userId,
                        cursor,
                        size
                );

        return ResponseEntity.ok()
                .header("Cache-Control", "private, no-store")
                .body(ApiResponse.success(response));
    }

    @PutMapping("/api/v1/laboratories/{laboratoryId}/bookmark")
    public ResponseEntity<Void> addBookmark(
            @PathVariable Long laboratoryId
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);

        bookmarkService.add(userId, laboratoryId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/laboratories/{laboratoryId}/bookmark")
    public ResponseEntity<Void> deleteBookmark(
            @PathVariable Long laboratoryId
    ) {
        Long userId = currentUserProvider.currentUserId()
                .orElseThrow(AccessTokenInvalidException::new);

        bookmarkService.remove(userId, laboratoryId);

        return ResponseEntity.noContent().build();
    }
}
