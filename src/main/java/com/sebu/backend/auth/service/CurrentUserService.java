package com.sebu.backend.auth.service;

import com.sebu.backend.auth.exception.AccessTokenInvalidException;
import com.sebu.backend.auth.exception.InvalidGradeException;
import com.sebu.backend.global.auth.CurrentUserProvider;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
        return CurrentUser.from(user);
    }

    @Transactional
    public CurrentUser updateGrade(Integer grade) {
        if (grade == null || grade < 1 || grade > 4) {
            throw new InvalidGradeException();
        }
        Long userId = currentUserProvider.currentUserId()
            .orElseThrow(AccessTokenInvalidException::new);
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(AccessTokenInvalidException::new);
        user.updateGrade(grade, LocalDateTime.now(ZoneOffset.UTC));
        return CurrentUser.from(user);
    }

    public record CurrentUser(
        Long id,
        String studentId,
        String name,
        Short grade,
        Department department,
        boolean profileCompleted
    ) {
        private static CurrentUser from(AppUser user) {
            var major = user.getMajorDepartment();
            Department department = user.getSejongDepartmentName() == null
                ? major == null ? null : new Department(major.getId(), major.getName())
                : new Department(null, user.getSejongDepartmentName());
            return new CurrentUser(
                user.getId(),
                user.getProviderUserId(),
                user.getName(),
                user.getGrade(),
                department,
                user.isProfileCompleted()
            );
        }

        public record Department(Long id, String name) {
        }
    }
}
