package com.sebu.backend.bookmark.dto;

import com.sebu.backend.mypage.dto.MyPageResponse;

import java.util.List;

public record BookmarkedLaboratoriesResponse(
        List<MyPageResponse.BookmarkedLaboratory> items,
        String nextCursor,
        boolean hasNext
) {
}
