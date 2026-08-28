ALTER TABLE laboratory_research_field_candidate
    ADD COLUMN split_from_candidate_id BIGINT NULL;

ALTER TABLE laboratory_research_field_candidate
    DROP CONSTRAINT ck_lrf_candidate_extraction_method;

ALTER TABLE laboratory_research_field_candidate
    ADD CONSTRAINT ck_lrf_candidate_extraction_method CHECK (
        extraction_method IN (
            'WHOLE_TEXT',
            'DELIMITED',
            'LONG_TEXT',
            'MANUAL_SPLIT'
        )
    );

ALTER TABLE laboratory_research_field_candidate
    ADD CONSTRAINT ck_lrf_candidate_split_origin CHECK (
        (
            extraction_method = 'MANUAL_SPLIT'
            AND split_from_candidate_id IS NOT NULL
        )
        OR
        (
            extraction_method <> 'MANUAL_SPLIT'
            AND split_from_candidate_id IS NULL
        )
    );

ALTER TABLE laboratory_research_field_candidate
    ADD CONSTRAINT ck_lrf_candidate_not_self_split CHECK (
        split_from_candidate_id IS NULL
        OR split_from_candidate_id <> id
    );

ALTER TABLE laboratory_research_field_candidate
    ADD CONSTRAINT fk_lrf_candidate_split_origin
        FOREIGN KEY (split_from_candidate_id)
        REFERENCES laboratory_research_field_candidate (id)
        ON DELETE RESTRICT;

CREATE INDEX idx_lrf_candidate_split_origin
    ON laboratory_research_field_candidate (split_from_candidate_id, id);
