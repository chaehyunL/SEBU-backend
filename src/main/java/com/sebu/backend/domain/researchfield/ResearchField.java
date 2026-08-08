package com.sebu.backend.domain.researchfield;

import com.sebu.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter @Entity @Table(name = "research_field") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResearchField extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    public ResearchField(String name) { this.name = name; }
}
