package com.sebu.backend.domain.department;

import com.sebu.backend.domain.college.College;
import com.sebu.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Entity
@Table(name = "department", uniqueConstraints = @UniqueConstraint(name = "uk_department_college_name", columnNames = {"college_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id", nullable = false) private College college;
    @Column(nullable = false, length = 100) private String name;
    public Department(College college, String name) { this.college = college; this.name = name; }
}
