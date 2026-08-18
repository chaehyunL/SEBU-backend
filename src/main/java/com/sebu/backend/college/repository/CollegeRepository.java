package com.sebu.backend.college.repository;
import com.sebu.backend.college.domain.College;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CollegeRepository extends JpaRepository<College, Long> { }
