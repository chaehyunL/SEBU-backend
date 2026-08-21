package com.sebu.backend.mypage.service;

import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.mypage.dto.ProfileResponse;
import com.sebu.backend.mypage.dto.ProfileUpdateRequest;
import com.sebu.backend.mypage.moderation.IntroductionModerationException;
import com.sebu.backend.mypage.moderation.IntroductionModerator;
import com.sebu.backend.mypage.moderation.ModerationResult;
import com.sebu.backend.user.domain.AppUser;
import com.sebu.backend.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;
    private final IntroductionModerator introductionModerator;

    @Transactional
    public ProfileResponse updateProfile(
            Long userId,
            ProfileUpdateRequest request
    ) {
        ModerationResult moderationResult =
                introductionModerator.moderate(request.introduction());

        if (!moderationResult.allowed()) {
            throw new IntroductionModerationException();
        }
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("USER_NOT_FOUND"));

        Long majorId;

        try {
            majorId = Long.parseLong(request.majorId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("INVALID_MAJOR_ID");
        }

        Department major = departmentRepository.findById(majorId)
                .orElseThrow(() ->
                        new IllegalArgumentException("MAJOR_NOT_FOUND"));

        String normalizedName = request.name().trim();

        user.updateProfile(
                normalizedName,
                request.grade(),
                major,
                request.gpaBand(),
                request.introduction(),
                LocalDateTime.now(),
                moderationResult.policyVersion(),
                moderationResult.providerVersion()
        );

        return toResponse(user);
    }

    private ProfileResponse toResponse(AppUser user) {
        Department major = user.getMajorDepartment();

        return new ProfileResponse(
                user.getName(),
                user.getGrade(),
                new ProfileResponse.Major(
                        major.getId().toString(),
                        major.getName()
                ),
                user.getGpaBand(),
                user.getIntroduction(),
                user.isProfileCompleted(),
                user.getProfileUpdatedAt()
        );
    }
}
