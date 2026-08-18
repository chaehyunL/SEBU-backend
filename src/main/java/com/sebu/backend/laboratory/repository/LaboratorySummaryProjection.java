package com.sebu.backend.laboratory.repository;

import com.sebu.backend.laboratory.domain.RecruitmentStatus;

public interface LaboratorySummaryProjection {
    Long getId();
    String getName();
    String getWebsiteUrl();
    Long getProfessorId();
    String getProfessorName();
    String getProfessorEmail();
    Long getCollegeId();
    String getCollegeName();
    Long getDepartmentId();
    String getDepartmentName();
    RecruitmentStatus getRecruitmentStatus();
    Long getBookmarkCount();
    Boolean getBookmarked();
}
