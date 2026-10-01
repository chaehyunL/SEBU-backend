-- Repair V48 mappings whose reviewed database names used a different separator.
-- These names and category choices come from the reviewed V45 research-field seed.
DROP TABLE IF EXISTS v49_robot_parent_mapping_replacement;
DROP TABLE IF EXISTS v49_candidate_name_correction;

INSERT INTO research_field_category_mapping (research_field_id, category_id)
SELECT field.id, category.id
FROM (
    SELECT 'AI 기반 건설로봇 운영' AS field_name,
           'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI' AS category_code
    UNION ALL SELECT '군집·편대 비행', 'ROBOT_AUTONOMOUS_AERIAL'
    UNION ALL SELECT '트랙터·트레일러 자율주행', 'ROBOT_AUTONOMOUS_MOBILITY'
) reviewed_mapping
JOIN research_field field ON field.name = reviewed_mapping.field_name
JOIN research_field_category category ON category.code = reviewed_mapping.category_code
LEFT JOIN research_field_category_mapping existing
    ON existing.research_field_id = field.id
   AND existing.category_id = category.id
WHERE existing.research_field_id IS NULL;

-- These fields now have specific second-level mappings, so remove their broad
-- direct assignment to the first-level category as V48 intended.
-- Materialize the IDs before deleting from the mapping table (MySQL target-table rule).
CREATE TABLE v49_robot_parent_mapping_replacement (
    research_field_id BIGINT NOT NULL PRIMARY KEY
);

INSERT INTO v49_robot_parent_mapping_replacement (research_field_id)
SELECT field.id
FROM (
    SELECT 'AI 기반 건설로봇 운영' AS field_name,
           'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI' AS category_code
    UNION ALL SELECT '군집·편대 비행', 'ROBOT_AUTONOMOUS_AERIAL'
    UNION ALL SELECT '트랙터·트레일러 자율주행', 'ROBOT_AUTONOMOUS_MOBILITY'
) reviewed_mapping
JOIN research_field field ON field.name = reviewed_mapping.field_name
JOIN research_field_category child ON child.code = reviewed_mapping.category_code
JOIN research_field_category parent
    ON parent.id = child.parent_category_id AND parent.code = 'ROBOT_AUTONOMOUS'
JOIN research_field_category_mapping child_mapping
    ON child_mapping.research_field_id = field.id
   AND child_mapping.category_id = child.id;

DELETE FROM research_field_category_mapping
WHERE category_id = (
    SELECT id FROM research_field_category WHERE code = 'ROBOT_AUTONOMOUS'
)
AND research_field_id IN (
    SELECT research_field_id FROM v49_robot_parent_mapping_replacement
);

DROP TABLE v49_robot_parent_mapping_replacement;

-- V47 matched candidate names literally. Match reviewed aliases after collapsing
-- the ASCII whitespace accepted by the promotion normalizer (space, tab, LF, VT,
-- FF, CR). Build the pattern with character codes to avoid MySQL/H2 escape differences.
-- MySQL CHAR() and CONCAT() produce a binary pattern; cast it to text because
-- MySQL 8.0.22+ regular-expression functions reject binary arguments.
-- The pattern has nine characters; specify the length to avoid H2 CHAR(1).
CREATE TABLE v49_candidate_name_correction (
    old_name VARCHAR(100) NOT NULL PRIMARY KEY,
    canonical_name VARCHAR(100) NOT NULL
);

INSERT INTO v49_candidate_name_correction (old_name, canonical_name) VALUES
    ('로봇 공학', '로봇공학'),
    ('무인 항공기(UAV)', '무인항공기(UAV)'),
    ('경로 계획 및 장애물 회피', '경로계획 및 장애물회피');

-- Preserve raw text, source identity, promotion references and review snapshots.
-- Do not conceal an unrelated promoted target or a deliberate candidate rename.
UPDATE laboratory_research_field_candidate
SET candidate_name = (
        SELECT correction.canonical_name
        FROM v49_candidate_name_correction correction
        WHERE correction.old_name = TRIM(REGEXP_REPLACE(
            laboratory_research_field_candidate.candidate_name,
            CAST(CONCAT('[ ', CHAR(9), CHAR(10), CHAR(11), CHAR(12), CHAR(13), ']+') AS CHAR(9)), ' '
        ))
    ),
    version = version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM v49_candidate_name_correction correction
    WHERE correction.old_name = TRIM(REGEXP_REPLACE(
        laboratory_research_field_candidate.candidate_name,
        CAST(CONCAT('[ ', CHAR(9), CHAR(10), CHAR(11), CHAR(12), CHAR(13), ']+') AS CHAR(9)), ' '
    ))
    AND (
        laboratory_research_field_candidate.promoted_research_field_id IS NULL
        OR EXISTS (
            SELECT 1 FROM research_field promoted
            WHERE promoted.id = laboratory_research_field_candidate.promoted_research_field_id
              AND promoted.name = correction.canonical_name
        )
    )
);

DROP TABLE v49_candidate_name_correction;
