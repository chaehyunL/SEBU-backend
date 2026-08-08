package com.sebu.backend.domain.college;

import com.sebu.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Entity @Table(name = "college") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class College extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    public College(String name) { this.name = name; }
}
