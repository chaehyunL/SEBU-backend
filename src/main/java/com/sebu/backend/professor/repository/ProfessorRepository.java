package com.sebu.backend.professor.repository;
import com.sebu.backend.professor.domain.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProfessorRepository extends JpaRepository<Professor, Long> { }
