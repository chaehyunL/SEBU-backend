package com.sebu.backend.auth.dto;

import com.sebu.backend.auth.service.CurrentUserService;

public record MeResponse(
    Long id,
    String nickname,
    String studentId,
    String name,
    Short grade,
    DepartmentResponse department,
    boolean profileCompleted
) {
    public static MeResponse from(CurrentUserService.CurrentUser user) {
        var department = user.department();
        return new MeResponse(
            user.id(),
            user.nickname(),
            user.studentId(),
            user.name(),
            user.grade(),
            department == null ? null : new DepartmentResponse(
                department.id(), department.name()
            ),
            user.profileCompleted()
        );
    }

    public record DepartmentResponse(Long id, String name) {
    }
}
