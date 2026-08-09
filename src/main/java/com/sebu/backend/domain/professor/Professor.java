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

    @Column(unique = true, length = 255)
    private String email;

    public Professor(Department department, String name, String email) {
        this.department = department;
        this.name = name;
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}
