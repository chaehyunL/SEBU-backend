package com.sebu.backend.community.common;

import com.sebu.backend.community.common.dto.CommunityAuthorResponse;
import com.sebu.backend.user.domain.AppUser;
import org.springframework.stereotype.Component;

@Component
public class CommunityAuthorMapper {
    private static final String ANONYMOUS_NICKNAME = "익명";

    public CommunityAuthorResponse toResponse(AppUser user) {
        return new CommunityAuthorResponse(
                user.getId(),
                user.getNickname() == null ? ANONYMOUS_NICKNAME : user.getNickname()
        );
    }

    public String displayNickname(AppUser user) {
        return user.getNickname() == null ? ANONYMOUS_NICKNAME : user.getNickname();
    }
}
