-- Normalize spacing-only variants in the robot/autonomous-systems taxonomy.
-- Keep crawl source text intact; update reviewed candidate/display names only.
DROP TABLE IF EXISTS v47_research_field_name_correction;
DROP TABLE IF EXISTS v47_research_field_merge;

CREATE TABLE v47_research_field_name_correction (
    old_name VARCHAR(100) NOT NULL,
    canonical_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (old_name)
);

CREATE TABLE v47_research_field_merge (
    source_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    PRIMARY KEY (source_id)
);

INSERT INTO v47_research_field_name_correction (old_name, canonical_name)
VALUES
    ('로봇 공학', '로봇공학'),
    ('무인 항공기(UAV)', '무인항공기(UAV)'),
    ('경로 계획 및 장애물 회피', '경로계획 및 장애물회피');

-- If both names exist, merge their links into the canonical row before deletion.
INSERT INTO v47_research_field_merge (source_id, target_id)
SELECT source.id, target.id
FROM v47_research_field_name_correction correction
JOIN research_field source ON source.name = correction.old_name
JOIN research_field target ON target.name = correction.canonical_name
WHERE source.id <> target.id;

-- Preserve laboratory links when the alias and canonical field both exist.
INSERT INTO laboratory_research_field (laboratory_id, research_field_id)
SELECT source_link.laboratory_id, merge_target.target_id
FROM laboratory_research_field source_link
JOIN v47_research_field_merge merge_target
    ON merge_target.source_id = source_link.research_field_id
LEFT JOIN laboratory_research_field target_link
    ON target_link.laboratory_id = source_link.laboratory_id
   AND target_link.research_field_id = merge_target.target_id
WHERE target_link.laboratory_id IS NULL;

-- Preserve category assignments when merging duplicate field rows.
INSERT INTO research_field_category_mapping (research_field_id, category_id)
SELECT merge_target.target_id, source_mapping.category_id
FROM research_field_category_mapping source_mapping
JOIN v47_research_field_merge merge_target
    ON merge_target.source_id = source_mapping.research_field_id
LEFT JOIN research_field_category_mapping target_mapping
    ON target_mapping.research_field_id = merge_target.target_id
   AND target_mapping.category_id = source_mapping.category_id
WHERE target_mapping.research_field_id IS NULL;

-- Canonicalize candidate labels while retaining raw_field_text and crawl provenance.
UPDATE laboratory_research_field_candidate
SET candidate_name = (
        SELECT correction.canonical_name
        FROM v47_research_field_name_correction correction
        WHERE correction.old_name = laboratory_research_field_candidate.candidate_name
    ),
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM v47_research_field_name_correction correction
    WHERE correction.old_name = laboratory_research_field_candidate.candidate_name
);

UPDATE laboratory_research_field_candidate
SET promoted_research_field_id = (
        SELECT merge_target.target_id
        FROM v47_research_field_merge merge_target
        WHERE merge_target.source_id =
            laboratory_research_field_candidate.promoted_research_field_id
    ),
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE promoted_research_field_id IN (
    SELECT source_id FROM v47_research_field_merge
);

-- Rename aliases when no canonical row exists, retaining the original field ID.
UPDATE research_field
SET name = (
        SELECT correction.canonical_name
        FROM v47_research_field_name_correction correction
        WHERE correction.old_name = research_field.name
    ),
    updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM v47_research_field_name_correction correction
    WHERE correction.old_name = research_field.name
)
AND id NOT IN (SELECT source_id FROM v47_research_field_merge);

DELETE FROM laboratory_research_field
WHERE research_field_id IN (SELECT source_id FROM v47_research_field_merge);

DELETE FROM research_field_category_mapping
WHERE research_field_id IN (SELECT source_id FROM v47_research_field_merge);

DELETE FROM research_field
WHERE id IN (SELECT source_id FROM v47_research_field_merge);

DROP TABLE v47_research_field_merge;
DROP TABLE v47_research_field_name_correction;
