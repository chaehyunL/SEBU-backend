CREATE TABLE laboratory_review (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   laboratory_id BIGINT NOT NULL,
                                   author_id BIGINT NOT NULL,

                                   overall_rating INT NOT NULL,
                                   research_intensity VARCHAR(30) NOT NULL,
                                   compensation VARCHAR(30) NOT NULL,
                                   paper_opportunity VARCHAR(30) NOT NULL,
                                   atmosphere VARCHAR(30) NOT NULL,

                                   content VARCHAR(2000) NOT NULL,

                                   participation_year INT NOT NULL,
                                   participation_term VARCHAR(30) NOT NULL,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   deleted_at TIMESTAMP NULL,

                                   CONSTRAINT pk_laboratory_review
                                       PRIMARY KEY (id),

                                   CONSTRAINT ck_laboratory_review_overall_rating
                                       CHECK (overall_rating BETWEEN 1 AND 5),

                                   CONSTRAINT ck_laboratory_review_research_intensity
                                       CHECK (research_intensity IN ('LOW', 'MEDIUM', 'HIGH')),

                                   CONSTRAINT ck_laboratory_review_compensation
                                       CHECK (compensation IN ('NONE', 'SMALL_AMOUNT', 'SUFFICIENT')),

                                   CONSTRAINT ck_laboratory_review_paper_opportunity
                                       CHECK (paper_opportunity IN ('NONE', 'AVERAGE', 'MANY')),

                                   CONSTRAINT ck_laboratory_review_atmosphere
                                       CHECK (atmosphere IN ('COMPETITIVE', 'NORMAL', 'COOPERATIVE')),

                                   CONSTRAINT ck_laboratory_review_participation_term
                                       CHECK (
                                           participation_term IN (
                                                                  'FIRST_SEMESTER',
                                                                  'SUMMER_BREAK',
                                                                  'SECOND_SEMESTER',
                                                                  'WINTER_BREAK'
                                               )
                                           ),

                                   CONSTRAINT ck_laboratory_review_participation_year
                                       CHECK (participation_year >= 2000),

                                   CONSTRAINT fk_laboratory_review_laboratory
                                       FOREIGN KEY (laboratory_id)
                                           REFERENCES laboratory (id)
                                           ON DELETE RESTRICT,

                                   CONSTRAINT fk_laboratory_review_author
                                       FOREIGN KEY (author_id)
                                           REFERENCES app_user (id)
                                           ON DELETE CASCADE
);
