ALTER TABLE laboratory_review
    ADD COLUMN category VARCHAR(30) NULL AFTER author_id;

UPDATE laboratory_review
SET category = 'OTHER'
WHERE category IS NULL;

ALTER TABLE laboratory_review
    MODIFY COLUMN category VARCHAR(30) NOT NULL;

ALTER TABLE laboratory_review
    ADD CONSTRAINT ck_laboratory_review_category
        CHECK (
            category IN (
                         'ACCEPTANCE',
                         'RESEARCH_ENVIRONMENT',
                         'PROFESSOR_STYLE',
                         'COMPENSATION_WELFARE',
                         'OTHER'
                )
            );

ALTER TABLE laboratory_review
DROP CONSTRAINT ck_laboratory_review_overall_rating;

ALTER TABLE laboratory_review
DROP COLUMN overall_rating;

ALTER TABLE laboratory_review
DROP CONSTRAINT ck_laboratory_review_paper_opportunity;

ALTER TABLE laboratory_review
DROP COLUMN paper_opportunity;

ALTER TABLE laboratory_review
    ADD CONSTRAINT uk_laboratory_review_author_laboratory
        UNIQUE (author_id, laboratory_id);

CREATE TABLE laboratory_review_tag (
                                       review_id BIGINT NOT NULL,
                                       tag VARCHAR(50) NOT NULL,

                                       CONSTRAINT pk_laboratory_review_tag
                                           PRIMARY KEY (review_id, tag),

                                       CONSTRAINT ck_laboratory_review_tag
                                           CHECK (
                                               tag IN (
                                                       'RESEARCH_IMMERSION',
                                                       'STUDY_RESEARCH_BALANCE',
                                                       'FREE_ATMOSPHERE',
                                                       'STRUCTURED_RESEARCH_GUIDANCE',
                                                       'PROFESSOR_COMMUNICATION',
                                                       'ACTIVE_FEEDBACK',
                                                       'PROJECT_OPPORTUNITY',
                                                       'DIVERSE_RESEARCH_EXPERIENCE',
                                                       'INTEREST_FIELD_RESEARCH',
                                                       'CAREER_CONNECTION'
                                                   )
                                               ),

                                       CONSTRAINT fk_laboratory_review_tag_review
                                           FOREIGN KEY (review_id)
                                               REFERENCES laboratory_review (id)
                                               ON DELETE CASCADE
);
