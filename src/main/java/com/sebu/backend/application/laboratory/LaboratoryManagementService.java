package com.sebu.backend.application.laboratory;

import com.sebu.backend.domain.bookmark.Bookmark;
import com.sebu.backend.domain.bookmark.BookmarkId;
import com.sebu.backend.domain.bookmark.BookmarkRepository;
import com.sebu.backend.domain.department.Department;
import com.sebu.backend.domain.department.DepartmentRepository;
import com.sebu.backend.domain.laboratory.Laboratory;
import com.sebu.backend.domain.laboratory.LaboratoryRepository;
import com.sebu.backend.domain.laboratory.RecruitmentStatus;
import com.sebu.backend.domain.professor.Professor;
import com.sebu.backend.domain.professor.ProfessorRepository;
import com.sebu.backend.domain.user.AppUser;
import com.sebu.backend.domain.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LaboratoryManagementService {
    private final LaboratoryRepository laboratoryRepository;
    private final ProfessorRepository professorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppUserRepository appUserRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public Laboratory create(
        Long professorId,
        Long departmentId,
        String name,
        String websiteUrl,
        RecruitmentStatus status
    ) {
        Professor professor = findProfessor(professorId);
        Department department = findDepartment(departmentId);
        validateSameDepartment(professor, department);
        validateUniqueActiveName(departmentId, name);
        return laboratoryRepository.save(new Laboratory(professor, department, name, websiteUrl, status));
    }

    @Transactional
    public void softDelete(Long laboratoryId) {
        findActiveLaboratory(laboratoryId).softDelete();
    }

    @Transactional
    public Bookmark addBookmark(Long userId, Long laboratoryId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Laboratory laboratory = findActiveLaboratory(laboratoryId);
        BookmarkId bookmarkId = new BookmarkId(userId, laboratoryId);
        if (bookmarkRepository.existsById(bookmarkId)) {
            throw new IllegalStateException("BOOKMARK_DUPLICATED");
        }
        return bookmarkRepository.save(new Bookmark(user, laboratory));
    }

    private Professor findProfessor(Long professorId) {
        return professorRepository.findById(professorId)
            .orElseThrow(() -> new IllegalArgumentException("Professor not found"));
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
            .orElseThrow(() -> new IllegalArgumentException("Department not found"));
    }

    private Laboratory findActiveLaboratory(Long laboratoryId) {
        return laboratoryRepository.findByIdAndDeletedAtIsNull(laboratoryId)
            .orElseThrow(() -> new IllegalArgumentException("Laboratory not found"));
    }

    private void validateSameDepartment(Professor professor, Department department) {
        if (!professor.getDepartment().getId().equals(department.getId())) {
            throw new IllegalArgumentException("PROFESSOR_DEPARTMENT_MISMATCH");
        }
    }

    private void validateUniqueActiveName(Long departmentId, String name) {
        if (laboratoryRepository.existsByDepartmentIdAndNameAndDeletedAtIsNull(departmentId, name)) {
            throw new IllegalStateException("ACTIVE_LABORATORY_NAME_DUPLICATED");
        }
    }
}
