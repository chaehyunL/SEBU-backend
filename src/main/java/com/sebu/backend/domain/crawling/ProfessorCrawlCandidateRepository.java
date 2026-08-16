package com.sebu.backend.domain.crawling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorCrawlCandidateRepository extends JpaRepository<ProfessorCrawlCandidate, Long> {
    List<ProfessorCrawlCandidate> findAllByReviewStatusAndStaleFalse(CandidateReviewStatus reviewStatus);
    List<ProfessorCrawlCandidate> findAllBySourceId(Long sourceId);
}
