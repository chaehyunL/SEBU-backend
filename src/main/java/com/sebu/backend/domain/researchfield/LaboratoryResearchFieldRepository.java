package com.sebu.backend.domain.researchfield;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface LaboratoryResearchFieldRepository extends JpaRepository<LaboratoryResearchField, LaboratoryResearchFieldId> {
    @Query("""
        select lrf.laboratory.id as laboratoryId, rf.name as name
        from LaboratoryResearchField lrf join lrf.researchField rf
        where lrf.laboratory.id in :laboratoryIds
        order by lrf.laboratory.id, rf.name
        """)
    List<LaboratoryResearchFieldProjection> findFieldsByLaboratoryIds(@Param("laboratoryIds") Collection<Long> laboratoryIds);
}
