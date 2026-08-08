package com.sebu.backend.domain.laboratory;

import com.sebu.backend.domain.bookmark.*;
import com.sebu.backend.domain.department.*;
import com.sebu.backend.domain.professor.*;
import com.sebu.backend.domain.user.*;
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
    public Laboratory create(Long professorId, Long departmentId, String name, String websiteUrl, RecruitmentStatus status) {
        Professor professor = professorRepository.findById(professorId).orElseThrow(() -> new IllegalArgumentException("Professor not found"));
        Department department = departmentRepository.findById(departmentId).orElseThrow(() -> new IllegalArgumentException("Department not found"));
        if (!professor.getDepartment().getId().equals(department.getId())) throw new IllegalArgumentException("PROFESSOR_DEPARTMENT_MISMATCH");
        if (laboratoryRepository.existsByDepartmentIdAndNameAndDeletedAtIsNull(departmentId, name)) throw new IllegalStateException("ACTIVE_LABORATORY_NAME_DUPLICATED");
        return laboratoryRepository.save(new Laboratory(professor, department, name, websiteUrl, status));
    }

    @Transactional
    public void softDelete(Long laboratoryId) {
        laboratoryRepository.findByIdAndDeletedAtIsNull(laboratoryId)
            .orElseThrow(() -> new IllegalArgumentException("Laboratory not found")).softDelete();
    }

    @Transactional
    public Bookmark addBookmark(Long userId, Long laboratoryId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Laboratory laboratory = laboratoryRepository.findByIdAndDeletedAtIsNull(laboratoryId)
            .orElseThrow(() -> new IllegalArgumentException("Laboratory not found"));
        BookmarkId id = new BookmarkId(userId, laboratoryId);
        if (bookmarkRepository.existsById(id)) throw new IllegalStateException("BOOKMARK_DUPLICATED");
        return bookmarkRepository.save(new Bookmark(user, laboratory));
    }
}
