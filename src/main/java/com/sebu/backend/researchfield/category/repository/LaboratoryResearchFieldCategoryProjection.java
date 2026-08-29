package com.sebu.backend.researchfield.category.repository;

public interface LaboratoryResearchFieldCategoryProjection {
    Long getLaboratoryId();

    Long getCategoryId();

    String getCategoryCode();

    String getCategoryName();
}
