-- Adds the reviewed second level beneath the existing robot/autonomous category.
DROP TABLE IF EXISTS v48_robot_category_seed;
DROP TABLE IF EXISTS v48_robot_field_category_seed;
DROP TABLE IF EXISTS v48_robot_parent_mapping_replacement;

ALTER TABLE research_field_category
    ADD COLUMN parent_category_id BIGINT NULL AFTER display_order;

ALTER TABLE research_field_category
    ADD CONSTRAINT fk_research_field_category_parent
        FOREIGN KEY (parent_category_id)
        REFERENCES research_field_category (id)
        ON DELETE RESTRICT;

CREATE TABLE v48_robot_category_seed (
    category_order INT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    PRIMARY KEY (code),
    UNIQUE KEY uk_v48_robot_category_seed_order (category_order)
);

INSERT INTO v48_robot_category_seed (category_order, code, name, description) VALUES
    (1, 'ROBOT_AUTONOMOUS_ROBOTICS', '로봇공학·메카트로닉스', '로봇공학, 로보틱스, 메카트로닉스 및 지능형 로봇'),
    (2, 'ROBOT_AUTONOMOUS_DESIGN', '로봇 설계·메커니즘', '로봇 메커니즘 설계 및 로봇용 물리 센서'),
    (3, 'ROBOT_AUTONOMOUS_CONTROL', '로봇 제어·매니퓰레이션', '로봇 제어, 매니퓰레이터, 조작 및 작업 학습'),
    (4, 'ROBOT_AUTONOMOUS_LEARNING', '로봇 학습·지능', '로봇 인공지능, 강화학습 및 작업 추론'),
    (5, 'ROBOT_AUTONOMOUS_PHYSICAL_AI', '피지컬 AI·인간-로봇 상호작용', '물리 환경과 상호작용하는 AI 및 인간-로봇 상호작용'),
    (6, 'ROBOT_AUTONOMOUS_SERVICE_HUMANOID', '서비스·휴머노이드 로봇', '서비스 로봇, 휴머노이드 및 사람을 위한 로봇 기술'),
    (7, 'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI', '건설·농업 로봇', '건설 및 농업 분야 로봇과 운영 기술'),
    (8, 'ROBOT_AUTONOMOUS_MOBILITY', '자율주행·지능형 모빌리티', '자율주행 및 지능형 이동체의 제어와 시스템'),
    (9, 'ROBOT_AUTONOMOUS_PLANNING', '자율주행 경로·행동 계획', '경로 계획, 거동 계획 및 장애물 회피'),
    (10, 'ROBOT_AUTONOMOUS_NAVIGATION', '항법·위치·자세 추정', '항법, 궤도 추적, 위치 추정 및 자세 추정'),
    (11, 'ROBOT_AUTONOMOUS_PERCEPTION', '환경 인식·객체 탐지', '이동체 주변 환경, 보행자 및 차량 인식'),
    (12, 'ROBOT_AUTONOMOUS_SENSOR_FUSION', '센서 융합·컴퓨터 비전', '카메라, 레이더, 라이다 등 센서 융합과 컴퓨터 비전'),
    (13, 'ROBOT_AUTONOMOUS_UAV', '드론·무인항공기', 'UAV, MAV, 드론 및 무인비행체 시스템'),
    (14, 'ROBOT_AUTONOMOUS_AERIAL', '군집·편대 비행·항공 모빌리티', '군집·편대 비행, 성층권 비행체 및 항공 모빌리티'),
    (15, 'ROBOT_AUTONOMOUS_MARITIME', '무인수상정·자율운항 선박', '무인수상정, 자율운항 선박 및 해양 이동체'),
    (16, 'ROBOT_AUTONOMOUS_UNDERWATER', '수중 로봇·무인잠수정', '수중 로봇, 무인잠수정 및 잠수함 자율화'),
    (17, 'ROBOT_AUTONOMOUS_UNMANNED_SYSTEMS', '무인이동체·유무인 복합체계', '무인이동체와 무인·유인 복합 운용 체계'),
    (18, 'ROBOT_AUTONOMOUS_EMBEDDED', '로봇 임베디드·펌웨어', '로봇 및 무인이동체 임베디드 시스템과 펌웨어'),
    (19, 'ROBOT_AUTONOMOUS_INDUSTRIAL', '산업·광산 자율운영', '산업 현장과 광산의 AI 기반 자율 운영'),
    (20, 'ROBOT_AUTONOMOUS_AUTOMOTIVE', '자동차 제어·운전자 지원', '자동차 제어, 군집주행 및 운전자 지원 시스템');

-- Resolve display orders against the current maximum so earlier catalogue additions remain intact.
INSERT INTO research_field_category (
    code,
    name,
    description,
    display_order,
    parent_category_id
)
SELECT
    seed.code,
    seed.name,
    seed.description,
    current_order.maximum_order + seed.category_order,
    parent.id
FROM v48_robot_category_seed seed
CROSS JOIN (
    SELECT COALESCE(MAX(display_order), 0) AS maximum_order
    FROM research_field_category
) current_order
JOIN research_field_category parent ON parent.code = 'ROBOT_AUTONOMOUS'
WHERE NOT EXISTS (
    SELECT 1 FROM research_field_category existing WHERE existing.code = seed.code
);

CREATE TABLE v48_robot_field_category_seed (
    field_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (field_name, category_code)
);

CREATE TABLE v48_robot_parent_mapping_replacement (
    research_field_id BIGINT NOT NULL,
    PRIMARY KEY (research_field_id)
);

INSERT INTO v48_robot_field_category_seed (field_name, category_code) VALUES
    ('강화학습기반 자율협력주행시스템', 'ROBOT_AUTONOMOUS_LEARNING'),
    ('강화학습기반 자율협력주행시스템', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('건설로봇', 'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI'),
    ('건설작업로봇', 'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI'),
    ('경로계획 및 장애물회피', 'ROBOT_AUTONOMOUS_PLANNING'),
    ('공중 관성항법 시스템', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('군집&편대 비행', 'ROBOT_AUTONOMOUS_AERIAL'),
    ('궤도 추적', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('극한환경 자율로봇 및 피지컬 AI', 'ROBOT_AUTONOMOUS_PHYSICAL_AI'),
    ('농업 로봇 및 AI', 'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI'),
    ('드론 펌웨어 개발 및 알고리즘 구현', 'ROBOT_AUTONOMOUS_UAV'),
    ('드론 펌웨어 개발 및 알고리즘 구현', 'ROBOT_AUTONOMOUS_EMBEDDED'),
    ('드론 환경 인식', 'ROBOT_AUTONOMOUS_PERCEPTION'),
    ('로보틱스', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('로보틱스용 물리 센서 개발', 'ROBOT_AUTONOMOUS_DESIGN'),
    ('로봇', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('로봇&메커니즘 디자인', 'ROBOT_AUTONOMOUS_DESIGN'),
    ('로봇공학', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('로봇 제어&로봇 매니퓰레이터', 'ROBOT_AUTONOMOUS_CONTROL'),
    ('로봇 조작 및 작업 학습', 'ROBOT_AUTONOMOUS_CONTROL'),
    ('로봇을 위한 인공지능 알고리즘', 'ROBOT_AUTONOMOUS_LEARNING'),
    ('메카트로닉스', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('무인수상정(USV)', 'ROBOT_AUTONOMOUS_MARITIME'),
    ('무인이동체 항법유도제어 센서 융합 및 자율주행항법 드론', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('무인이동체 항법유도제어 센서 융합 및 자율주행항법 드론', 'ROBOT_AUTONOMOUS_SENSOR_FUSION'),
    ('무인이동체 항법유도제어 센서 융합 및 자율주행항법 드론', 'ROBOT_AUTONOMOUS_UAV'),
    ('무인잠수정(AUV)', 'ROBOT_AUTONOMOUS_UNDERWATER'),
    ('무인체계 및 지능로봇 분야 피지컬 AI 유무인복합체계 외', 'ROBOT_AUTONOMOUS_PHYSICAL_AI'),
    ('무인체계 및 지능로봇 분야 피지컬 AI 유무인복합체계 외', 'ROBOT_AUTONOMOUS_UNMANNED_SYSTEMS'),
    ('무인체계 및 지능로봇 분야 피지컬 AI 유무인복합체계 외', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('무인항공기(UAV)', 'ROBOT_AUTONOMOUS_UAV'),
    ('물리적 AI(Embodied AI)', 'ROBOT_AUTONOMOUS_PHYSICAL_AI'),
    ('보행자 및 차량 객체 인식', 'ROBOT_AUTONOMOUS_PERCEPTION'),
    ('서비스로봇', 'ROBOT_AUTONOMOUS_SERVICE_HUMANOID'),
    ('선박제어알고리즘', 'ROBOT_AUTONOMOUS_MARITIME'),
    ('선박 충돌회피', 'ROBOT_AUTONOMOUS_MARITIME'),
    ('성층권 HALE&HAPS', 'ROBOT_AUTONOMOUS_AERIAL'),
    ('수중 차량 및 로봇', 'ROBOT_AUTONOMOUS_UNDERWATER'),
    ('위성항법', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('인간-로봇 상호작용', 'ROBOT_AUTONOMOUS_PHYSICAL_AI'),
    ('인공신경망기반 첨단운전자보조시스템', 'ROBOT_AUTONOMOUS_AUTOMOTIVE'),
    ('인류를 위한 로봇 기술 개발', 'ROBOT_AUTONOMOUS_SERVICE_HUMANOID'),
    ('자동차 제어&군집주행', 'ROBOT_AUTONOMOUS_AUTOMOTIVE'),
    ('자세 추정', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('자율운항선박', 'ROBOT_AUTONOMOUS_MARITIME'),
    ('자율주행 모빌리티 제어', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('자율주행 차량 거동계획', 'ROBOT_AUTONOMOUS_PLANNING'),
    ('자율주행을 위한 라이다와 카메라 기반 환경 인지', 'ROBOT_AUTONOMOUS_PERCEPTION'),
    ('자율주행을 위한 라이다와 카메라 기반 환경 인지', 'ROBOT_AUTONOMOUS_SENSOR_FUSION'),
    ('자율주행자동차 환경 인식', 'ROBOT_AUTONOMOUS_PERCEPTION'),
    ('자율주행차 경로계획', 'ROBOT_AUTONOMOUS_PLANNING'),
    ('자율주행차 제어', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('작업 상황 이해 및 추론', 'ROBOT_AUTONOMOUS_LEARNING'),
    ('잠수함 및 수중운동체 자율화', 'ROBOT_AUTONOMOUS_UNDERWATER'),
    ('지능형 로봇', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('지능형 모빌리티 시스템 및 제어', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('지능형 모빌리티 제어', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('지능형 이동체 인공지능', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('지능형 이동체 인공지능', 'ROBOT_AUTONOMOUS_LEARNING'),
    ('지능형 탐색', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('카메라*레이더*라이다 센서 융합', 'ROBOT_AUTONOMOUS_SENSOR_FUSION'),
    ('컴퓨터 비전 및 제어 시스템', 'ROBOT_AUTONOMOUS_PERCEPTION'),
    ('트랙터*트레일러 자율주행', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('피지컬AI', 'ROBOT_AUTONOMOUS_PHYSICAL_AI'),
    ('항법', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('항법시스템', 'ROBOT_AUTONOMOUS_NAVIGATION'),
    ('해양 무인이동체', 'ROBOT_AUTONOMOUS_MARITIME'),
    ('휴머노이드 로봇', 'ROBOT_AUTONOMOUS_SERVICE_HUMANOID'),
    ('AI기반 건설로봇 운영', 'ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI'),
    ('AI 기반 무인비행체', 'ROBOT_AUTONOMOUS_UAV'),
    ('AI 에이전트 기반 자율 광산 운영', 'ROBOT_AUTONOMOUS_INDUSTRIAL'),
    ('AI로봇', 'ROBOT_AUTONOMOUS_ROBOTICS'),
    ('MAV', 'ROBOT_AUTONOMOUS_UAV'),
    ('Solar UAV', 'ROBOT_AUTONOMOUS_UAV'),
    ('UAM', 'ROBOT_AUTONOMOUS_AERIAL'),
    ('UAM', 'ROBOT_AUTONOMOUS_MOBILITY'),
    ('UAV', 'ROBOT_AUTONOMOUS_UAV');

-- Add child mappings only for fields that already exist; classification never creates fields.
INSERT INTO research_field_category_mapping (research_field_id, category_id)
SELECT field.id, category.id
FROM v48_robot_field_category_seed seed
JOIN research_field field ON field.name = seed.field_name
JOIN research_field_category category ON category.code = seed.category_code
LEFT JOIN research_field_category_mapping existing
    ON existing.research_field_id = field.id
   AND existing.category_id = category.id
WHERE existing.research_field_id IS NULL;

-- Replace the broad parent mapping only when a more specific child mapping was added.
INSERT INTO v48_robot_parent_mapping_replacement (research_field_id)
SELECT DISTINCT parent_mapping.research_field_id
FROM research_field_category_mapping parent_mapping
JOIN research_field_category parent
    ON parent.id = parent_mapping.category_id
JOIN research_field_category_mapping child_mapping
    ON child_mapping.research_field_id = parent_mapping.research_field_id
JOIN research_field_category child
    ON child.id = child_mapping.category_id
WHERE parent.code = 'ROBOT_AUTONOMOUS'
  AND child.parent_category_id = parent.id;

DELETE FROM research_field_category_mapping
WHERE category_id = (
    SELECT id FROM research_field_category WHERE code = 'ROBOT_AUTONOMOUS'
)
AND research_field_id IN (
    SELECT research_field_id FROM v48_robot_parent_mapping_replacement
);

DROP TABLE v48_robot_parent_mapping_replacement;
DROP TABLE v48_robot_field_category_seed;
DROP TABLE v48_robot_category_seed;
