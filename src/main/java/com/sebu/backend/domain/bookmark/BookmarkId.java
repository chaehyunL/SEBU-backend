package com.sebu.backend.domain.bookmark;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Getter @EqualsAndHashCode @Embeddable @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class BookmarkId implements Serializable {
    private Long userId;
    private Long laboratoryId;
}
