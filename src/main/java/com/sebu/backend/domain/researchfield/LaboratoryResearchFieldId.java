package com.sebu.backend.domain.researchfield;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Getter @EqualsAndHashCode @Embeddable @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class LaboratoryResearchFieldId implements Serializable {
    private Long laboratoryId;
    private Long researchFieldId;
}
