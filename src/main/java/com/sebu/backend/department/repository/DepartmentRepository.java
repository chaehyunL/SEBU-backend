package com.sebu.backend.department.repository;
import com.sebu.backend.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DepartmentRepository extends JpaRepository<Department, Long> { }
