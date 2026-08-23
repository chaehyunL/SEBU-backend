package com.sebu.backend.promotion.service;

import com.sebu.backend.crawling.domain.ProfessorCrawlCandidate;
import com.sebu.backend.crawling.repository.CrawlSourceRepository;
import com.sebu.backend.crawling.repository.ProfessorCrawlCandidateRepository;
import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import com.sebu.backend.laboratory.domain.Laboratory;
import com.sebu.backend.laboratory.domain.LaboratoryNameSource;
import com.sebu.backend.laboratory.domain.RecruitmentStatus;
import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.professor.domain.Professor;
import com.sebu.backend.professor.repository.ProfessorRepository;
import com.sebu.backend.promotion.dto.PromotionResult;
import com.sebu.backend.promotion.exception.CandidatePromotionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProfessorCandidatePromotionService {
    private final ProfessorCrawlCandidateRepository candidateRepository;
    private final CrawlSourceRepository sourceRepository;
    private final ProfessorRepository professorRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final DepartmentRepository departmentRepository;
    private final TransactionTemplate transactionTemplate;

    public ProfessorCandidatePromotionService(
        ProfessorCrawlCandidateRepository candidateRepository,
        CrawlSourceRepository sourceRepository,
        ProfessorRepository professorRepository,
        LaboratoryRepository laboratoryRepository,
        DepartmentRepository departmentRepository,
        PlatformTransactionManager transactionManager
    ) {
        this.candidateRepository = candidateRepository;
        this.sourceRepository = sourceRepository;
        this.professorRepository = professorRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.departmentRepository = departmentRepository;
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public PromotionResult promote(Long sourceId) {
        validateSource(sourceId);
        List<Long> candidateIds = findCandidateIds(sourceId);
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<PromotionResult.Failure> failures = new ArrayList<>();

        for (Long candidateId : candidateIds) {
            try {
                PromotionAction action = transactionTemplate.execute(
                    status -> promoteCandidate(candidateId)
                );
                if (action == PromotionAction.CREATED) {
                    createdCount++;
                } else if (action == PromotionAction.UPDATED) {
                    updatedCount++;
                } else {
                    skippedCount++;
                }
            } catch (RuntimeException exception) {
                failures.add(new PromotionResult.Failure(
                    candidateId,
                    failureReason(exception),
                    exception
                ));
            }
        }

        return new PromotionResult(
            candidateIds.size(),
            createdCount,
            updatedCount,
            skippedCount,
            failures
        );
    }

    private PromotionAction promoteCandidate(Long candidateId) {
        ProfessorCrawlCandidate candidate = candidateRepository.findByIdForPromotion(candidateId)
            .orElse(null);
        if (candidate == null) {
            return PromotionAction.SKIPPED;
        }
        if (!candidate.hasConsistentPromotionState()) {
            throw new CandidatePromotionException("INVALID_CANDIDATE_PROMOTION_STATE");
        }
        if (!candidate.needsPromotion()) {
            return PromotionAction.SKIPPED;
        }
        Department department = lockDepartment(candidate);
        if (candidate.hasBeenPromoted()) {
            return updatePromotedCandidate(candidate, department);
        }
        return createPromotedCandidate(candidate, department);
    }

    private PromotionAction createPromotedCandidate(
        ProfessorCrawlCandidate candidate,
        Department department
    ) {
        PromotionLaboratoryName laboratoryName = resolveLaboratoryName(candidate);
        ensureCreateHasNoConflict(candidate, department, laboratoryName.name());

        Professor professor = professorRepository.save(new Professor(
            department,
            candidate.getProfessorName(),
            candidate.getPosition(),
            candidate.getEmail()
        ));
        Laboratory laboratory = laboratoryRepository.save(new Laboratory(
            professor,
            department,
            laboratoryName.name(),
            candidate.getHomepageUrl(),
            candidate.getResearchIntroduction(),
            RecruitmentStatus.UNKNOWN,
            laboratoryName.source()
        ));
        candidate.recordPromotion(professor, laboratory, LocalDateTime.now());
        candidateRepository.save(candidate);
        return PromotionAction.CREATED;
    }

    private PromotionAction updatePromotedCandidate(
        ProfessorCrawlCandidate candidate,
        Department department
    ) {
        if (candidate.getPromotedProfessor() == null || candidate.getPromotedLaboratory() == null) {
            throw new CandidatePromotionException("PROMOTED_ENTITY_WAS_REMOVED");
        }

        Professor professor = professorRepository.findByIdForUpdate(
                candidate.getPromotedProfessor().getId()
            )
            .orElseThrow(() -> new CandidatePromotionException("PROMOTED_PROFESSOR_NOT_FOUND"));
        Laboratory laboratory = laboratoryRepository.findByIdForUpdate(
                candidate.getPromotedLaboratory().getId()
            )
            .orElseThrow(() -> new CandidatePromotionException("PROMOTED_LABORATORY_NOT_FOUND"));
        if (laboratory.isDeleted()) {
            throw new CandidatePromotionException("PROMOTED_LABORATORY_IS_DELETED");
        }

        ensureOwnedByDepartment(professor, laboratory, department);
        PromotionLaboratoryName laboratoryName = resolveLaboratoryName(candidate);
        ensureUpdateHasNoConflict(candidate, professor, laboratory, department, laboratoryName.name());

        boolean changed = !professor.hasPromotionProfile(
            candidate.getProfessorName(),
            candidate.getPosition(),
            candidate.getEmail()
        ) || !laboratory.hasPromotionDetails(
            laboratoryName.name(),
            candidate.getHomepageUrl(),
            candidate.getResearchIntroduction(),
            laboratoryName.source()
        );
        if (changed) {
            professor.updateFromPromotion(
                candidate.getProfessorName(),
                candidate.getPosition(),
                candidate.getEmail()
            );
            laboratory.updateFromPromotion(
                laboratoryName.name(),
                candidate.getHomepageUrl(),
                candidate.getResearchIntroduction(),
                laboratoryName.source()
            );
        }
        candidate.recordPromotion(professor, laboratory, LocalDateTime.now());
        return changed ? PromotionAction.UPDATED : PromotionAction.SKIPPED;
    }

    private void ensureCreateHasNoConflict(
        ProfessorCrawlCandidate candidate,
        Department department,
        String laboratoryName
    ) {
        Long departmentId = requireDepartmentId(department);
        if (candidate.getEmail() != null && professorRepository.existsByEmail(candidate.getEmail())) {
            throw new CandidatePromotionException("PROFESSOR_EMAIL_CONFLICT");
        }
        if (laboratoryRepository.existsByDepartmentIdAndNameAndDeletedAtIsNull(
            departmentId,
            laboratoryName
        )) {
            throw new CandidatePromotionException("LABORATORY_NAME_CONFLICT");
        }
    }

    private void ensureUpdateHasNoConflict(
        ProfessorCrawlCandidate candidate,
        Professor professor,
        Laboratory laboratory,
        Department department,
        String laboratoryName
    ) {
        Long departmentId = requireDepartmentId(department);
        if (candidate.getEmail() != null && professorRepository.existsByEmailAndIdNot(
            candidate.getEmail(),
            professor.getId()
        )) {
            throw new CandidatePromotionException("PROFESSOR_EMAIL_CONFLICT");
        }
        if (laboratoryRepository.existsByDepartmentIdAndNameAndDeletedAtIsNullAndIdNot(
            departmentId,
            laboratoryName,
            laboratory.getId()
        )) {
            throw new CandidatePromotionException("LABORATORY_NAME_CONFLICT");
        }
    }

    private void ensureOwnedByDepartment(
        Professor professor,
        Laboratory laboratory,
        Department department
    ) {
        Long departmentId = requireDepartmentId(department);
        if (!Objects.equals(professor.getDepartment().getId(), departmentId)
            || !Objects.equals(laboratory.getDepartment().getId(), departmentId)
            || !Objects.equals(laboratory.getProfessor().getId(), professor.getId())) {
            throw new CandidatePromotionException("PROMOTED_ENTITY_OWNERSHIP_CONFLICT");
        }
    }

    private PromotionLaboratoryName resolveLaboratoryName(ProfessorCrawlCandidate candidate) {
        if (candidate.getLaboratoryName() != null) {
            return new PromotionLaboratoryName(
                candidate.getLaboratoryName(),
                LaboratoryNameSource.OFFICIAL
            );
        }
        return new PromotionLaboratoryName(
            candidate.getProfessorName() + " 교수님 연구실",
            LaboratoryNameSource.GENERATED
        );
    }

    private List<Long> findCandidateIds(Long sourceId) {
        if (sourceId == null) {
            return candidateRepository.findPromotionCandidateIds();
        }
        return candidateRepository.findPromotionCandidateIdsBySourceId(sourceId);
    }

    private void validateSource(Long sourceId) {
        if (sourceId != null && !sourceRepository.existsById(sourceId)) {
            throw new CandidatePromotionException("CRAWL_SOURCE_NOT_FOUND: " + sourceId);
        }
    }

    private Long requireDepartmentId(Department department) {
        Long departmentId = department.getId();
        if (departmentId == null) {
            throw new CandidatePromotionException("SOURCE_DEPARTMENT_NOT_PERSISTED");
        }
        return departmentId;
    }

    private Department lockDepartment(ProfessorCrawlCandidate candidate) {
        Long departmentId = requireDepartmentId(candidate.getSource().getDepartment());
        return departmentRepository.findByIdForUpdate(departmentId)
            .orElseThrow(() -> new CandidatePromotionException("SOURCE_DEPARTMENT_NOT_FOUND"));
    }

    private String failureReason(RuntimeException exception) {
        if (exception instanceof DataIntegrityViolationException) {
            return "PROMOTION_DATA_CONFLICT";
        }
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private enum PromotionAction {
        CREATED,
        UPDATED,
        SKIPPED
    }

    private record PromotionLaboratoryName(String name, LaboratoryNameSource source) {
    }
}
