package com.sebu.backend.domain.professor;

import com.sebu.backend.domain.common.BaseTimeEntity;
import com.sebu.backend.domain.department.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Getter
@Entity
@Table(name = "professor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Professor extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String position;

    @Column(unique = true, length = 255)
    private String email;

    public Professor(Department department, String name, String email) {
        this(department, name, null, email);
    }

    public Professor(Department department, String name, String position, String email) {
        this.department = department;
        this.name = name;
        this.position = normalizeNullable(position);
        this.email = normalizeEmail(email);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
