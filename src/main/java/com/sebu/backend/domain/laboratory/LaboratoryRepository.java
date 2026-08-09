package com.sebu.backend.domain.laboratory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {
    boolean existsByDepartmentIdAndNameAndDeletedAtIsNull(Long departmentId, String name);
    Optional<Laboratory> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        select l.id as id, l.name as name, l.websiteUrl as websiteUrl,
               p.id as professorId, p.name as professorName, p.email as professorEmail,
               c.id as collegeId, c.name as collegeName,
               d.id as departmentId, d.name as departmentName,
               l.recruitmentStatus as recruitmentStatus,
               count(distinct b.user.id) as bookmarkCount,
               case when count(distinct case when b.user.id = :userId then b.user.id else null end) > 0
                    then true else false end as bookmarked
        from Laboratory l
        join l.professor p join l.department d join d.college c
        left join Bookmark b on b.laboratory = l
        where l.deletedAt is null
        group by l.id, l.name, l.websiteUrl, p.id, p.name, p.email,
                 c.id, c.name, d.id, d.name, l.recruitmentStatus
        order by l.id
        """)
    List<LaboratorySummaryProjection> findAllSummaries(@Param("userId") Long userId);
}
