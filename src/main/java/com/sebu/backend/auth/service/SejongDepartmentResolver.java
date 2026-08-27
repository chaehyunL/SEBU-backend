package com.sebu.backend.auth.service;

import com.sebu.backend.department.domain.Department;
import com.sebu.backend.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SejongDepartmentResolver {
    private static final Logger log = LoggerFactory.getLogger(SejongDepartmentResolver.class);

    private final DepartmentRepository departmentRepository;

    public Department resolve(String departmentName) {
        List<Department> matches = departmentRepository.findAllByName(departmentName.trim());
        if (matches.size() == 1) {
            return matches.getFirst();
        }
        log.warn("Could not uniquely resolve Sejong department: matchCount={}", matches.size());
        return null;
    }
}
