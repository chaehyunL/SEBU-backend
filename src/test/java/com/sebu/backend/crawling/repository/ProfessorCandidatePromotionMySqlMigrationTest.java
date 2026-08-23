package com.sebu.backend.crawling.repository;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class ProfessorCandidatePromotionMySqlMigrationTest {
    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 8, 23, 10, 0);
    private static final LocalDateTime PROMOTED_AT = LocalDateTime.of(2026, 8, 23, 11, 0);

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void v14DataSurvivesV15AndPromotionConstraintsAreEnforced() throws Exception {
        migrateToV14();

        FixtureIds fixture;
        try (Connection connection = connection()) {
            fixture = insertV14Fixture(connection);
        }

        migrateToV15();

        try (Connection connection = connection()) {
            assertExistingLaboratoryWasBackfilledAsOfficial(connection, fixture.laboratoryId());
            assertApprovedCandidateRemainsUnchangedAndUnpromoted(connection, fixture.candidateId());
            assertInvalidNameSourceIsRejected(connection, fixture.laboratoryId());
            assertNameSourceHasNoDefault(connection);

            markCandidatePromoted(connection, fixture);
            deleteLaboratory(connection, fixture.laboratoryId());
            assertLaboratoryReferenceWasClearedWithoutLosingPromotionHistory(
                connection,
                fixture.candidateId(),
                fixture.professorId()
            );
        }
    }

    private void migrateToV14() {
        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("14"))
            .load()
            .migrate();
    }

    private void migrateToV15() {
        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("15"))
            .load()
            .migrate();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
            MYSQL.getJdbcUrl(),
            MYSQL.getUsername(),
            MYSQL.getPassword()
        );
    }

    private FixtureIds insertV14Fixture(Connection connection) throws SQLException {
        long collegeId = insertAndReturnId(
            connection,
            "INSERT INTO college (name) VALUES (?)",
            "승격 마이그레이션 테스트 대학"
        );
        long departmentId = insertAndReturnId(
            connection,
            "INSERT INTO department (college_id, name) VALUES (?, ?)",
            collegeId,
            "승격 마이그레이션 테스트 학과"
        );
        long professorId = insertAndReturnId(
            connection,
            "INSERT INTO professor (department_id, name, position, email) VALUES (?, ?, ?, ?)",
            departmentId,
            "기존교수",
            "교수",
            "existing-professor@example.com"
        );
        long laboratoryId = insertAndReturnId(
            connection,
            """
                INSERT INTO laboratory (
                    professor_id,
                    department_id,
                    name,
                    website_url,
                    description,
                    recruitment_status
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
            professorId,
            departmentId,
            "기존 공식 연구실",
            "https://example.com/existing-lab",
            "기존 연구실 소개",
            "UNKNOWN"
        );
        long sourceId = insertAndReturnId(
            connection,
            """
                INSERT INTO crawl_source (
                    department_id,
                    source_name,
                    source_url,
                    parser_type,
                    is_active,
                    last_crawl_status,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            departmentId,
            "승격 마이그레이션 테스트 교수진",
            "https://example.com/promotion-migration/professors",
            "SEJONG_STANDARD",
            true,
            "SUCCESS",
            0L
        );
        long candidateId = insertAndReturnId(
            connection,
            """
                INSERT INTO professor_crawl_candidate (
                    source_id,
                    source_identity_key,
                    professor_name,
                    position,
                    email,
                    laboratory_name,
                    research_introduction,
                    homepage_url,
                    source_url_at_crawl,
                    parser_type_at_crawl,
                    is_stale,
                    review_status,
                    reviewed_by,
                    reviewed_at,
                    crawled_at,
                    version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            sourceId,
            "email:approved-candidate@example.com",
            "승인후보교수",
            "교수",
            "approved-candidate@example.com",
            null,
            "검수 완료된 연구 소개",
            "https://example.com/approved-candidate",
            "https://example.com/promotion-migration/professors",
            "SEJONG_STANDARD",
            false,
            "APPROVED",
            "migration-tester",
            Timestamp.valueOf(REVIEWED_AT),
            Timestamp.valueOf(REVIEWED_AT.minusHours(1)),
            0L
        );
        return new FixtureIds(professorId, laboratoryId, candidateId);
    }

    private void assertExistingLaboratoryWasBackfilledAsOfficial(
        Connection connection,
        long laboratoryId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT name, name_source
            FROM laboratory
            WHERE id = ?
            """)) {
            statement.setLong(1, laboratoryId);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("name")).isEqualTo("기존 공식 연구실");
                assertThat(result.getString("name_source")).isEqualTo("OFFICIAL");
            }
        }
    }

    private void assertApprovedCandidateRemainsUnchangedAndUnpromoted(
        Connection connection,
        long candidateId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT review_status,
                   review_revision,
                   laboratory_name,
                   promoted_professor_id,
                   promoted_laboratory_id,
                   promoted_at,
                   promoted_reviewed_at,
                   promoted_review_revision
            FROM professor_crawl_candidate
            WHERE id = ?
            """)) {
            statement.setLong(1, candidateId);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("review_status")).isEqualTo("APPROVED");
                assertThat(result.getLong("review_revision")).isOne();
                assertThat(result.getString("laboratory_name")).isNull();
                assertThat(result.getObject("promoted_professor_id")).isNull();
                assertThat(result.getObject("promoted_laboratory_id")).isNull();
                assertThat(result.getTimestamp("promoted_at")).isNull();
                assertThat(result.getTimestamp("promoted_reviewed_at")).isNull();
                assertThat(result.getObject("promoted_review_revision")).isNull();
            }
        }
    }

    private void assertInvalidNameSourceIsRejected(
        Connection connection,
        long laboratoryId
    ) {
        assertThatThrownBy(() -> executeUpdate(
            connection,
            "UPDATE laboratory SET name_source = ? WHERE id = ?",
            "UNKNOWN_SOURCE",
            laboratoryId
        )).isInstanceOf(SQLException.class);
    }

    private void assertNameSourceHasNoDefault(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT column_default
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'laboratory'
              AND column_name = 'name_source'
            """)) {
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("column_default")).isNull();
            }
        }
    }

    private void markCandidatePromoted(
        Connection connection,
        FixtureIds fixture
    ) throws SQLException {
        int updated = executeUpdate(
            connection,
            """
                UPDATE professor_crawl_candidate
                SET promoted_professor_id = ?,
                    promoted_laboratory_id = ?,
                    promoted_at = ?,
                    promoted_reviewed_at = ?,
                    promoted_review_revision = review_revision
                WHERE id = ?
                """,
            fixture.professorId(),
            fixture.laboratoryId(),
            Timestamp.valueOf(PROMOTED_AT),
            Timestamp.valueOf(REVIEWED_AT),
            fixture.candidateId()
        );
        assertThat(updated).isOne();
    }

    private void deleteLaboratory(Connection connection, long laboratoryId) throws SQLException {
        int deleted = executeUpdate(
            connection,
            "DELETE FROM laboratory WHERE id = ?",
            laboratoryId
        );
        assertThat(deleted).isOne();
    }

    private void assertLaboratoryReferenceWasClearedWithoutLosingPromotionHistory(
        Connection connection,
        long candidateId,
        long professorId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT review_status,
                   review_revision,
                   laboratory_name,
                   promoted_professor_id,
                   promoted_laboratory_id,
                   promoted_at,
                   promoted_reviewed_at,
                   promoted_review_revision
            FROM professor_crawl_candidate
            WHERE id = ?
            """)) {
            statement.setLong(1, candidateId);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("review_status")).isEqualTo("APPROVED");
                assertThat(result.getLong("review_revision")).isOne();
                assertThat(result.getString("laboratory_name")).isNull();
                assertThat(result.getLong("promoted_professor_id")).isEqualTo(professorId);
                assertThat(result.getObject("promoted_laboratory_id")).isNull();
                assertThat(result.getTimestamp("promoted_at")).isNotNull();
                assertThat(result.getTimestamp("promoted_reviewed_at")).isNotNull();
                assertThat(result.getLong("promoted_review_revision")).isOne();
            }
        }
    }

    private long insertAndReturnId(
        Connection connection,
        String sql,
        Object... parameters
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            sql,
            Statement.RETURN_GENERATED_KEYS
        )) {
            setParameters(statement, parameters);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Generated key was not returned");
                }
                return keys.getLong(1);
            }
        }
    }

    private int executeUpdate(
        Connection connection,
        String sql,
        Object... parameters
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, parameters);
            return statement.executeUpdate();
        }
    }

    private void setParameters(
        PreparedStatement statement,
        Object... parameters
    ) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }

    private record FixtureIds(long professorId, long laboratoryId, long candidateId) {
    }
}
