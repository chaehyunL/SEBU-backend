package com.sebu.backend.laboratory.repository;

import com.sebu.backend.laboratory.domain.LaboratoryDepartment;
import com.sebu.backend.laboratory.domain.LaboratoryDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LaboratoryDepartmentRepository
    extends JpaRepository<LaboratoryDepartment, LaboratoryDepartmentId> {

    boolean existsByLaboratory_IdAndDepartment_Id(Long laboratoryId, Long departmentId);

    @Query("""
        select case when count(affiliation) > 0 then true else false end
        from LaboratoryDepartment affiliation
        join affiliation.laboratory laboratory
        where affiliation.department.id = :departmentId
          and laboratory.name = :name
          and laboratory.deletedAt is null
          and (:excludedLaboratoryId is null or laboratory.id <> :excludedLaboratoryId)
        """)
    boolean existsActiveLaboratoryName(
        @Param("departmentId") Long departmentId,
        @Param("name") String name,
        @Param("excludedLaboratoryId") Long excludedLaboratoryId
    );
}
