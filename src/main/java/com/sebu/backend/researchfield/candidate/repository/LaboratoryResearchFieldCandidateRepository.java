package com.sebu.backend.researchfield.candidate.repository;

import com.sebu.backend.researchfield.candidate.domain.LaboratoryResearchFieldCandidate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LaboratoryResearchFieldCandidateRepository
    extends JpaRepository<LaboratoryResearchFieldCandidate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select candidate
        from LaboratoryResearchFieldCandidate candidate
        where candidate.laboratory.id = :laboratoryId
        order by candidate.sourceOrder, candidate.id
        """)
    List<LaboratoryResearchFieldCandidate> findAllByLaboratoryIdForUpdate(
        @Param("laboratoryId") Long laboratoryId
    );
}
