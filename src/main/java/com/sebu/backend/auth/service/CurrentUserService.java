package com.sebu.backend.auth.service;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final CurrentUserProvider currentUserProvider;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public CurrentUser getCurrentUser() {
        Long userId = currentUserProvider.currentUserId()
            .orElseThrow(AccessTokenInvalidException::new);
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(AccessTokenInvalidException::new);
        return new CurrentUser(user.getId(), null, user.isProfileCompleted());
    }

    public record CurrentUser(Long id, String nickname, boolean profileCompleted) {
    }
}
