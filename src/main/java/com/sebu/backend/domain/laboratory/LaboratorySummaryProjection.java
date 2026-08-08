package com.sebu.backend.domain.laboratory;
public interface LaboratorySummaryProjection {
    Long getId(); String getName(); String getWebsiteUrl();
    Long getProfessorId(); String getProfessorName(); String getProfessorEmail();
    Long getCollegeId(); String getCollegeName();
    Long getDepartmentId(); String getDepartmentName();
    RecruitmentStatus getRecruitmentStatus(); Long getBookmarkCount(); Boolean getBookmarked();
}
