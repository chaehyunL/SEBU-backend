package com.sebu.backend.crawling.repository;

import com.sebu.backend.crawling.domain.CandidateReviewStatus;
import com.sebu.backend.crawling.domain.ProfessorCrawlCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorCrawlCandidateRepository extends JpaRepository<ProfessorCrawlCandidate, Long> {
    List<ProfessorCrawlCandidate> findAllByReviewStatusAndStaleFalse(CandidateReviewStatus reviewStatus);
    List<ProfessorCrawlCandidate> findAllBySourceId(Long sourceId);
}
