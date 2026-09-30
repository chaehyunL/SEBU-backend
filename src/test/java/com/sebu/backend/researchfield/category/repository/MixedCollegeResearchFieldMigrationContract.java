package com.sebu.backend.researchfield.category.repository;

import com.sebu.backend.laboratory.repository.LaboratoryRepository;
import com.sebu.backend.laboratory.repository.LaboratoryResearchFieldRepository;
import com.sebu.backend.researchfield.candidate.domain.ResearchFieldCandidateDraft;
import com.sebu.backend.researchfield.candidate.domain.ResearchFieldExtractionMethod;
import com.sebu.backend.researchfield.candidate.repository.LaboratoryResearchFieldCandidateRepository;
import com.sebu.backend.researchfield.promotion.service.LaboratoryResearchFieldLinkService;
import com.sebu.backend.researchfield.promotion.service.ResearchFieldCandidatePromotionService;
import com.sebu.backend.researchfield.promotion.service.ResearchFieldCandidatePromotionTransactionService;
import com.sebu.backend.researchfield.promotion.service.ResearchFieldNameNormalizer;
import com.sebu.backend.researchfield.promotion.service.ResearchFieldPromotionTargetResolver;
import com.sebu.backend.researchfield.repository.ResearchFieldRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class MixedCollegeResearchFieldMigrationContract {
    private static final String TARGET_EMAIL = "kbjeong7@sejong.ac.kr";
    private static final String MIGRATION = "db/migration/V45__import_reviewed_mixed_college_research_fields.sql";
    protected JdbcTemplate jdbc;

    protected abstract DataSource dataSource();

    @BeforeEach
    void cleanDedicatedDatabase() {
        jdbc = new JdbcTemplate(dataSource());
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        assertThat(database).startsWith("sebu_field80_test");
        flyway("45").clean();
    }

    @Test
    void blankDatabaseImportsReviewedFieldsAndMappingsWithoutCandidateAuditData() {
        flyway("45").migrate();

        assertThat(count("research_field_category")).isEqualTo(32);
        assertThat(count("research_field")).isEqualTo(981);
        assertThat(count("laboratory_research_field")).isEqualTo(1072);
        assertThat(count("research_field_category_mapping")).isEqualTo(1223);
        assertThat(count("laboratory_research_field_candidate")).isZero();
        assertThat(count("professor_crawl_candidate")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT laboratory_id) FROM laboratory_research_field", Integer.class)).isEqualTo(272);
        assertNoUnmappedOrDuplicatePairs();
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM laboratory_research_field lf JOIN laboratory l ON l.id=lf.laboratory_id
            JOIN professor p ON p.id=l.professor_id WHERE p.email='clee@sejong.ac.kr'
            """, Integer.class)).isZero();
        assertThat(jdbc.queryForList("""
            SELECT f.name FROM laboratory_research_field lf JOIN research_field f ON f.id=lf.research_field_id
            JOIN laboratory l ON l.id=lf.laboratory_id JOIN professor p ON p.id=l.professor_id
            WHERE p.email='sufyanahmedali@sejong.ac.kr' ORDER BY f.name
            """, String.class)).containsExactlyInAnyOrder("유도", "항법", "추정 및 추적");
        assertCategory("천체물리 분광학", "PHYSICS_ASTRONOMY");
        assertCategory("중국문학", "LANGUAGE_LITERATURE");
        assertCategory("탄소나노소재 열전도성 복합재료", "CHEMISTRY_MATERIALS");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM research_field f JOIN research_field_category_mapping m ON m.research_field_id=f.id
            JOIN research_field_category c ON c.id=m.category_id
            WHERE f.name='탄소나노소재 열전도성 복합재료' AND c.code='SIGNAL_MEDIA'
            """, Integer.class)).isZero();
        validateLatestHibernate();
        flyway("45").validate();
        assertThat(flyway("45").migrate().migrationsExecuted).isZero();
    }

    @ParameterizedTest(name = "V49: {0} -> {1}")
    @CsvSource({
        "AI 기반 건설로봇 운영, ROBOT_AUTONOMOUS_CONSTRUCTION_AGRI",
        "군집·편대 비행, ROBOT_AUTONOMOUS_AERIAL",
        "트랙터·트레일러 자율주행, ROBOT_AUTONOMOUS_MOBILITY"
    })
    void v49MapsEachExistingDatabaseNameToItsReviewedRobotSubcategory(
        String fieldName, String categoryCode
    ) {
        assertThat(new ClassPathResource(
            "db/migration/V49__repair_robot_mappings_and_candidate_spacing.sql"
        ).exists()).as("V49 보정 마이그레이션이 배포 리소스에 포함되어야 한다").isTrue();
        flyway("48").migrate();

        // Use the actual V45 data rather than creating fields from the V48 mapping list.
        Long fieldId = jdbc.queryForObject(
            "SELECT id FROM research_field WHERE name=?", Long.class, fieldName
        );
        List<Long> laboratoryIds = jdbc.queryForList("""
            SELECT laboratory_id FROM laboratory_research_field
            WHERE research_field_id=? ORDER BY laboratory_id
            """, Long.class, fieldId);
        assertThat(laboratoryIds).isNotEmpty();
        assertThat(countCategoryMapping(fieldName, categoryCode))
            .as("V48에서 누락된 연결: %s -> %s", fieldName, categoryCode).isZero();
        assertThat(countParentMapping(fieldName)).isEqualTo(1);

        assertThat(flyway("49").migrate().migrationsExecuted).isEqualTo(1);

        assertCategory(fieldName, categoryCode);
        assertThat(countParentMapping(fieldName)).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT id FROM research_field WHERE name=?", Long.class, fieldName
        )).isEqualTo(fieldId);
        assertThat(jdbc.queryForList("""
            SELECT laboratory_id FROM laboratory_research_field
            WHERE research_field_id=? ORDER BY laboratory_id
            """, Long.class, fieldId)).isEqualTo(laboratoryIds);
        assertThat(jdbc.queryForObject("""
            SELECT parent.code FROM research_field_category child
            JOIN research_field_category parent ON parent.id=child.parent_category_id
            WHERE child.code=?
            """, String.class, categoryCode)).isEqualTo("ROBOT_AUTONOMOUS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"로봇  공학", "로봇\t \t공학"})
    void v49AllowsRePromotionAfterSourceReappears(String originalName) {
        flyway("46").migrate();
        ResearchFieldNameNormalizer normalizer = new ResearchFieldNameNormalizer();
        assertThat(normalizer.normalize(originalName)).isEqualTo("로봇 공학");

        Long researchFieldId = jdbc.queryForObject(
            "SELECT id FROM research_field WHERE name=?", Long.class, normalizer.normalize(originalName)
        );
        Long laboratoryId = jdbc.queryForObject("""
            SELECT laboratory_id FROM laboratory_research_field
            WHERE research_field_id=? ORDER BY laboratory_id LIMIT 1
            """, Long.class, researchFieldId);
        jdbc.update("""
            INSERT INTO laboratory_research_field_candidate (
                id, laboratory_id, source_field_key, source_description_hash,
                raw_field_text, candidate_name, extraction_method, source_order,
                extraction_rule_version, review_status, reviewed_by, reviewed_at,
                review_revision, extracted_at, version, promoted_research_field_id,
                promoted_at, promoted_reviewed_at, promoted_review_revision
            ) VALUES (
                999999, ?, REPEAT('a', 64), REPEAT('b', 64), ?,
                ?, 'WHOLE_TEXT', 0, 'test', 'APPROVED', 'reviewer',
                CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 4, ?, CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP, 1
            )
            """, laboratoryId, originalName, originalName, researchFieldId);

        flyway("48").migrate();
        Long canonicalFieldId = jdbc.queryForObject(
            "SELECT id FROM research_field WHERE name='로봇공학'", Long.class
        );
        assertThat(jdbc.queryForObject("""
            SELECT candidate_name FROM laboratory_research_field_candidate WHERE id=999999
            """, String.class)).isEqualTo(originalName);
        assertThat(normalizer.equivalent(originalName, "로봇공학")).isFalse();
        String auditQuery = """
            SELECT raw_field_text, source_field_key, source_description_hash,
                   promoted_research_field_id, review_revision, promoted_review_revision,
                   reviewed_at, promoted_at, promoted_reviewed_at, review_status
            FROM laboratory_research_field_candidate WHERE id=999999
            """;
        Map<String, Object> auditBefore = jdbc.queryForMap(auditQuery);
        Long versionBefore = jdbc.queryForObject("""
            SELECT version FROM laboratory_research_field_candidate WHERE id=999999
            """, Long.class);

        assertThat(flyway("49").migrate().migrationsExecuted).isEqualTo(1);

        Map<String, Object> candidate = jdbc.queryForMap("""
            SELECT candidate_name, raw_field_text, version, promoted_research_field_id
            FROM laboratory_research_field_candidate WHERE id=999999
            """);
        assertThat(candidate.get("candidate_name")).isEqualTo("로봇공학");
        assertThat(candidate.get("raw_field_text")).isEqualTo(originalName);
        assertThat(((Number) candidate.get("version")).longValue()).isEqualTo(versionBefore + 1);
        assertThat(((Number) candidate.get("promoted_research_field_id")).longValue())
            .isEqualTo(canonicalFieldId);
        assertThat(jdbc.queryForMap(auditQuery)).isEqualTo(auditBefore);

        int fieldCount = count("research_field");
        int linkCount = count("laboratory_research_field");
        rePromoteMigratedCandidate(originalName, canonicalFieldId);
        assertThat(count("research_field")).isEqualTo(fieldCount);
        assertThat(count("laboratory_research_field")).isEqualTo(linkCount);
        assertThat(jdbc.queryForObject("""
            SELECT promoted_review_revision FROM laboratory_research_field_candidate WHERE id=999999
            """, Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("""
            SELECT promoted_research_field_id FROM laboratory_research_field_candidate WHERE id=999999
            """, Long.class)).isEqualTo(canonicalFieldId);
        assertThat(jdbc.queryForObject("""
            SELECT raw_field_text FROM laboratory_research_field_candidate WHERE id=999999
            """, String.class)).isEqualTo(originalName);
    }

    private void rePromoteMigratedCandidate(String originalName, Long canonicalFieldId) {
        var factory = configuredEntityManagerFactory();
        try {
            factory.afterPropertiesSet();
            var entityManager = factory.getObject().createEntityManager();
            try {
                // Use one explicit database transaction for the actual repositories and services.
                var transaction = entityManager.getTransaction();
                transaction.begin();
                try {
                    var repositories = new JpaRepositoryFactory(entityManager);
                    var candidates = repositories.getRepository(LaboratoryResearchFieldCandidateRepository.class);
                    var laboratories = repositories.getRepository(LaboratoryRepository.class);
                    var normalizer = new ResearchFieldNameNormalizer();
                    var transactionService = new ResearchFieldCandidatePromotionTransactionService(
                        candidates,
                        laboratories,
                        new ResearchFieldPromotionTargetResolver(
                            repositories.getRepository(ResearchFieldRepository.class), normalizer
                        ),
                        new LaboratoryResearchFieldLinkService(
                            repositories.getRepository(LaboratoryResearchFieldRepository.class)
                        ),
                        normalizer
                    );
                    var service = new ResearchFieldCandidatePromotionService(
                        candidates, laboratories, transactionService
                    );
                    var candidate = candidates.findByIdForPromotion(999999L).orElseThrow();
                    candidate.markStale();
                    candidate.refreshFromExtraction(
                        new ResearchFieldCandidateDraft(
                            candidate.getSourceFieldKey(), originalName, originalName,
                            ResearchFieldExtractionMethod.WHOLE_TEXT, 0
                        ),
                        "c".repeat(64), "test-v2", LocalDateTime.now()
                    );
                    candidate.approve("reviewer", "재승인", LocalDateTime.now());
                    assertThat(candidate.getCandidateName()).isEqualTo("로봇공학");
                    assertThat(candidate.needsPromotion()).isTrue();

                    var result = service.promote(candidate.getLaboratory().getId());

                    assertThat(result.failures()).isEmpty();
                    assertThat(result.candidateCount()).isEqualTo(1);
                    assertThat(result.createdFieldCount()).isZero();
                    assertThat(result.createdLinkCount()).isZero();
                    assertThat(result.promotedCount()).isEqualTo(1);
                    assertThat(result.skippedCount()).isZero();
                    assertThat(candidate.getPromotedResearchField().getId()).isEqualTo(canonicalFieldId);
                    assertThat(candidate.getPromotedReviewRevision()).isEqualTo(2L);
                    assertThat(candidate.needsPromotion()).isFalse();
                    transaction.commit();
                } finally {
                    if (transaction.isActive()) transaction.rollback();
                }
            } finally {
                entityManager.close();
            }
        } finally {
            factory.destroy();
        }
    }

    private int countParentMapping(String fieldName) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM research_field_category_mapping mapping
            JOIN research_field field ON field.id=mapping.research_field_id
            JOIN research_field_category category ON category.id=mapping.category_id
            WHERE field.name=? AND category.code='ROBOT_AUTONOMOUS'
            """, Integer.class, fieldName);
    }

    @Test
    void upgradeReusesDifferentIdsAndPreservesProfilesAuditRowsAndExistingMappings() {
        flyway("43").migrate();
        long department = jdbc.queryForObject("SELECT id FROM department WHERE name='건축공학과'", Long.class);
        jdbc.update("INSERT INTO professor (id,department_id,name,email) VALUES (90001,?,'정광복',?)", department,TARGET_EMAIL);
        jdbc.update("""
            INSERT INTO laboratory (id,professor_id,department_id,name,description,recruitment_status,name_source)
            VALUES (90002,90001,?,'검수된 공식 연구실','직접 작성한 소개','RECRUITING','OFFICIAL')
            """,department);
        flyway("44").migrate();
        jdbc.update("INSERT INTO research_field (id,name) VALUES (80001,'인공지능'),(80002,'사용자 정의 보존 분야')");
        jdbc.update("INSERT INTO research_field_category (id,code,name,description,display_order) VALUES (80003,'ARCHITECTURE_CIVIL','건축·토목·도시','기존 관리자 설명',400)");
        jdbc.update("INSERT INTO laboratory_research_field (laboratory_id,research_field_id) VALUES (90002,80002)");
        jdbc.update("INSERT INTO research_field_category_mapping (research_field_id,category_id) SELECT 80002,id FROM research_field_category WHERE code='AI_ML'");
        jdbc.update("""
            INSERT INTO laboratory_research_field_candidate
            (laboratory_id,source_field_key,source_description_hash,raw_field_text,candidate_name,
             extraction_method,source_order,extraction_rule_version,extracted_at,version)
            VALUES (90002,?,?,'검수 대기 원문','검수 대기 분야','WHOLE_TEXT',0,'test-v1',CURRENT_TIMESTAMP,0)
            ""","a".repeat(64),"b".repeat(64));
        var preserved = snapshotProfilesAndAudit();
        var oldFields = jdbc.queryForList("SELECT * FROM research_field ORDER BY id");
        var oldCategory = jdbc.queryForMap("SELECT * FROM research_field_category WHERE id=80003");

        assertThat(flyway("45").migrate().migrationsExecuted).isEqualTo(1);

        assertThat(snapshotProfilesAndAudit()).isEqualTo(preserved);
        assertThat(jdbc.queryForList("SELECT * FROM research_field WHERE id IN (80001,80002) ORDER BY id")).isEqualTo(oldFields);
        assertThat(jdbc.queryForMap("SELECT * FROM research_field_category WHERE id=80003")).isEqualTo(oldCategory);
        assertThat(jdbc.queryForObject("SELECT id FROM research_field WHERE name='인공지능'",Long.class)).isEqualTo(80001);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM laboratory_research_field WHERE laboratory_id=90002",Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM laboratory_research_field WHERE laboratory_id=90002 AND research_field_id=80002",Integer.class)).isEqualTo(1);
        assertCategory("사용자 정의 보존 분야","AI_ML");
        assertNoUnmappedOrDuplicatePairs();
        validateLatestHibernate();
    }

    @Test
    void replayDoesNotChangeIdsTimestampsOrMappings() {
        flyway("45").migrate();
        var before = snapshotCanonical();
        var replay = new ResourceDatabasePopulator(new ClassPathResource(MIGRATION));
        replay.setSqlScriptEncoding("UTF-8");
        replay.execute(dataSource());
        replay.execute(dataSource());
        assertThat(snapshotCanonical()).isEqualTo(before);
        assertNoUnmappedOrDuplicatePairs();
        flyway("45").validate();
    }

    @Test
    void wrongProfessorNameFailsBeforeCanonicalWrites() {
        flyway("44").migrate();
        jdbc.update("UPDATE professor SET name='다른 사람' WHERE email=?",TARGET_EMAIL);
        assertGuardedFailure();
    }

    @Test
    void softDeletedLaboratoryIsNotResurrected() {
        flyway("44").migrate();
        jdbc.update("UPDATE laboratory SET deleted_at=CURRENT_TIMESTAMP WHERE professor_id=(SELECT id FROM professor WHERE email=?)",TARGET_EMAIL);
        assertGuardedFailure();
    }

    @Test
    void multipleActiveLaboratoriesAreNotGuessed() {
        flyway("44").migrate();
        jdbc.update("""
            INSERT INTO laboratory (professor_id,department_id,name,recruitment_status,name_source)
            SELECT id,department_id,'또 다른 공식 연구실','UNKNOWN','OFFICIAL' FROM professor WHERE email=?
            """,TARGET_EMAIL);
        assertGuardedFailure();
    }

    @Test
    void ambiguousNullEmailProfessorIsNotGuessed() {
        flyway("44").migrate();
        jdbc.update("INSERT INTO professor (department_id,name,email) SELECT department_id,name,NULL FROM professor WHERE name='김홍범' AND email IS NULL");
        assertGuardedFailure();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void conflictingCategoryCodeOrNameFailsBeforeCanonicalWrites(boolean sameCode) {
        flyway("44").migrate();
        jdbc.update("INSERT INTO research_field_category (code,name,description,display_order) VALUES (?,?, '관리자 분류',400)",
            sameCode ? "ARCHITECTURE_CIVIL" : "CUSTOM_ARCH", sameCode ? "서로 다른 분류" : "건축·토목·도시");
        assertGuardedFailure();
    }

    @Test
    void missingReferencedCategoryFailsInsteadOfDroppingMappings() {
        flyway("44").migrate();
        jdbc.update("DELETE FROM research_field_category WHERE code='AI_ML'");
        assertGuardedFailure();
    }

    private void assertGuardedFailure() {
        var before = snapshotCanonical();
        assertThatThrownBy(() -> flyway("45").migrate()).isInstanceOf(RuntimeException.class);
        assertThat(snapshotCanonical()).isEqualTo(before);
    }

    private void assertNoUnmappedOrDuplicatePairs() {
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM research_field f WHERE NOT EXISTS
            (SELECT 1 FROM research_field_category_mapping m WHERE m.research_field_id=f.id)
            """, Integer.class)).isZero();
        for (String query : List.of(
            "SELECT COUNT(*) FROM (SELECT laboratory_id,research_field_id FROM laboratory_research_field GROUP BY laboratory_id,research_field_id HAVING COUNT(*)>1) duplicates",
            "SELECT COUNT(*) FROM (SELECT research_field_id,category_id FROM research_field_category_mapping GROUP BY research_field_id,category_id HAVING COUNT(*)>1) duplicates",
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'v45_%'")) {
            assertThat(jdbc.queryForObject(query, Integer.class)).isZero();
        }
    }

    private void assertCategory(String field, String code) {
        assertThat(countCategoryMapping(field, code))
            .as("연구 분야별 카테고리 연결: %s -> %s", field, code).isEqualTo(1);
    }

    private int countCategoryMapping(String field, String code) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM research_field f JOIN research_field_category_mapping m ON m.research_field_id=f.id
            JOIN research_field_category c ON c.id=m.category_id WHERE f.name=? AND c.code=?
            """, Integer.class, field, code);
    }

    private Map<String, Object> snapshotProfilesAndAudit() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String table : List.of(
            "college", "department", "professor", "laboratory", "app_user",
            "professor_crawl_candidate", "laboratory_research_field_candidate"
        )) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        return result;
    }

    private Map<String, Object> snapshotCanonical() {
        var result = snapshotProfilesAndAudit();
        result.put("fields", jdbc.queryForList("SELECT * FROM research_field ORDER BY id"));
        result.put("categories", jdbc.queryForList("SELECT * FROM research_field_category ORDER BY id"));
        result.put("labFields", jdbc.queryForList("SELECT * FROM laboratory_research_field ORDER BY laboratory_id,research_field_id"));
        result.put("categoryMappings", jdbc.queryForList("SELECT * FROM research_field_category_mapping ORDER BY research_field_id,category_id"));
        return result;
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
            .dataSource(dataSource())
            .locations("classpath:db/migration")
            .target(target)
            .cleanDisabled(false)
            .load();
    }

    private void validateLatestHibernate() {
        Flyway.configure().dataSource(dataSource()).locations("classpath:db/migration").load().migrate();
        var factory = configuredEntityManagerFactory();
        try {
            factory.afterPropertiesSet();
            assertThat(factory.getObject()).isNotNull();
        } finally {
            factory.destroy();
        }
    }

    private LocalContainerEntityManagerFactoryBean configuredEntityManagerFactory() {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource());
        factory.setPackagesToScan("com.sebu.backend");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
            "hibernate.hbm2ddl.auto", "validate",
            "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        ));
        return factory;
    }
}
