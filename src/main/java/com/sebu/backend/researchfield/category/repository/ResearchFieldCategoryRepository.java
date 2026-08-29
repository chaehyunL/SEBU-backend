package com.sebu.backend.researchfield.category.repository;

import com.sebu.backend.researchfield.category.domain.ResearchFieldCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ResearchFieldCategoryRepository
    extends JpaRepository<ResearchFieldCategory, Long> {

    List<ResearchFieldCategory> findAllByOrderByDisplayOrderAscIdAsc();

    @Query(value = """
        SELECT DISTINCT
            laboratory_field.laboratory_id AS laboratoryId,
            category.id AS categoryId,
            category.code AS categoryCode,
            category.name AS categoryName
        FROM laboratory_research_field laboratory_field
        JOIN research_field_category_mapping mapping
          ON mapping.research_field_id = laboratory_field.research_field_id
        JOIN research_field_category category
          ON category.id = mapping.category_id
        WHERE laboratory_field.laboratory_id IN (:laboratoryIds)
        ORDER BY laboratory_field.laboratory_id,
                 category.display_order,
                 category.id
        """, nativeQuery = true)
    List<LaboratoryResearchFieldCategoryProjection> findAllByLaboratoryIds(
        @Param("laboratoryIds") Collection<Long> laboratoryIds
    );
}
