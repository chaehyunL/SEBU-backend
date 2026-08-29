package com.sebu.backend.laboratory.repository;

public interface LaboratoryResearchFieldCategoryProjection {
    Long getLaboratoryId();

    Long getCategoryId();

    String getCategoryCode();

    String getCategoryName();

    Integer getDisplayOrder();
}
