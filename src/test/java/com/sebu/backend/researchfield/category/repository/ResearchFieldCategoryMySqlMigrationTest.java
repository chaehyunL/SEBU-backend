package com.sebu.backend.researchfield.category.repository;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class ResearchFieldCategoryMySqlMigrationTest {
    private static final int EXPECTED_CATEGORY_COUNT = 21;
    private static final int EXPECTED_CURATED_FIELD_COUNT = 700;
    private static final Path BASE_CLASSIFICATION_CSV = Path.of(
        "docs",
        "data",
        "research-field-category-classification.csv"
    );
    private static final Path NATURAL_SCIENCE_CLASSIFICATION_CSV = Path.of(
        "docs",
        "data",
        "natural-science-research-field-category-classification.csv"
    );

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @BeforeEach
    void cleanDatabase() {
        flyway(null).clean();
    }

    @Test
    void blankDatabaseReceivesAllCuratedCategoriesAndMappings() throws Exception {
        flyway(null).migrate();

        try (Connection connection = connection()) {
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category"))
                .isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertThat(count(
                connection,
                "SELECT COUNT(DISTINCT research_field_id) FROM research_field_category_mapping"
            )).isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertThat(count(
                connection,
                "SELECT COUNT(DISTINCT code) FROM research_field_category"
            )).isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(
                connection,
                "SELECT COUNT(DISTINCT name) FROM research_field_category"
            )).isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(
                connection,
                "SELECT COUNT(DISTINCT display_order) FROM research_field_category"
            )).isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(
                connection,
                """
                    SELECT COUNT(*)
                    FROM research_field_category category
                    LEFT JOIN research_field_category_mapping mapping
                      ON mapping.category_id = category.id
                    WHERE mapping.category_id IS NULL
                    """
            )).isZero();

            assertThatThrownBy(() -> executeUpdate(
                connection,
                """
                    INSERT INTO research_field_category (
                        code,
                        name,
                        description,
                        display_order
                    ) VALUES (?, ?, ?, ?)
                    """,
                "AI_ML",
                "중복 카테고리",
                "중복 코드는 허용되지 않는다",
                99
            )).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void v31UpgradeAddsNaturalScienceCategoriesAndMappings() throws Exception {
        flyway("31").migrate();

        long existingImageProcessingId;
        try (Connection connection = connection()) {
            existingImageProcessingId = findFieldId(connection, "이미지 처리");
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category"))
                .isEqualTo(18L);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isEqualTo(535L);
        }

        flyway(null).migrate();

        try (Connection connection = connection()) {
            assertThat(findFieldId(connection, "이미지 처리"))
                .isEqualTo(existingImageProcessingId);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category"))
                .isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertMapping(connection, "가사이드 이론", "MATH_STATISTICS");
            assertMapping(connection, "우주론", "PHYSICS_ASTRONOMY");
            assertMapping(connection, "고분자 열역학", "CHEMISTRY_MATERIALS");
        }
    }

    @Test
    void v22UpgradeMapsEveryCuratedFieldAndPreservesUnknownFields()
        throws Exception {
        flyway("22").migrate();
        List<CuratedAssignment> curatedAssignments = readCuratedAssignments();
        List<String> curatedFieldNames = expectedAssignments(curatedAssignments)
            .keySet()
            .stream()
            .toList();
        assertThat(curatedFieldNames)
            .hasSize(EXPECTED_CURATED_FIELD_COUNT)
            .doesNotHaveDuplicates();

        long existingSeedFieldId;
        long customFieldId;
        try (Connection connection = connection()) {
            insertFields(connection, curatedFieldNames);
            existingSeedFieldId = findFieldId(connection, "인공지능");
            customFieldId = insertAndReturnId(
                connection,
                "INSERT INTO research_field (name) VALUES (?)",
                "V22 기존 사용자 정의 연구 분야"
            );
        }

        flyway(null).migrate();

        try (Connection connection = connection()) {
            assertThat(findFieldId(connection, "인공지능")).isEqualTo(existingSeedFieldId);
            assertThat(findFieldId(connection, "V22 기존 사용자 정의 연구 분야"))
                .isEqualTo(customFieldId);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isEqualTo(EXPECTED_CURATED_FIELD_COUNT + 1L);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertThat(count(
                connection,
                "SELECT COUNT(DISTINCT research_field_id) FROM research_field_category_mapping"
            )).isEqualTo(EXPECTED_CURATED_FIELD_COUNT);
            assertThat(count(
                connection,
                """
                    SELECT COUNT(*)
                    FROM research_field_category category
                    LEFT JOIN research_field_category_mapping mapping
                      ON mapping.category_id = category.id
                    WHERE mapping.category_id IS NULL
                    """
            )).isZero();
            assertThat(count(
                connection,
                """
                    SELECT COUNT(*)
                    FROM research_field_category_mapping
                    WHERE research_field_id = ?
                    """,
                customFieldId
            )).isZero();
            assertMapping(connection, "인공지능", "AI_ML");
            assertMapping(connection, "의료 인공지능", "BIOMED_HEALTH");
            assertMapping(connection, "양자 컴퓨팅 알고리즘 (quantum algorithm)", "QUANTUM_TECH");
            assertMapping(connection, "메타버스 보안", "SECURITY_CRYPTO");
            assertMapping(connection, "로봇공학", "ROBOT_AUTONOMOUS");
            assertMapping(connection, "5G/6G 시스템", "COMM_NETWORK");
            assertMapping(connection, "가사이드 이론", "MATH_STATISTICS");
            assertMapping(connection, "우주론", "PHYSICS_ASTRONOMY");
            assertMapping(connection, "고분자 열역학", "CHEMISTRY_MATERIALS");
            assertMapping(
                connection,
                "광결정 디바이스(화학 센서·반사형 디스플레이·레이저)",
                "PHOTONICS_OPTICS"
            );
            assertThat(findAllMappings(connection))
                .isEqualTo(expectedAssignments(curatedAssignments));
            assertThatThrownBy(() -> executeUpdate(
                connection,
                "DELETE FROM research_field_category WHERE code = ?",
                "AI_ML"
            )).isInstanceOf(SQLException.class);
        }
    }

    private List<CuratedAssignment> readCuratedAssignments() throws Exception {
        List<CuratedAssignment> assignments = new ArrayList<>();
        assignments.addAll(readCuratedAssignments(BASE_CLASSIFICATION_CSV, 0, 5));
        assignments.addAll(readCuratedAssignments(NATURAL_SCIENCE_CLASSIFICATION_CSV, 1, 7));
        return assignments;
    }

    private List<CuratedAssignment> readCuratedAssignments(
        Path csvPath,
        int fieldNameColumn,
        int categoryCodeColumn
    ) throws Exception {
        return Files.readAllLines(csvPath, StandardCharsets.UTF_8).stream()
            .skip(1)
            .map(this::parseCsvRow)
            .map(columns -> new CuratedAssignment(
                columns.get(fieldNameColumn),
                columns.get(categoryCodeColumn)
            ))
            .toList();
    }

    private List<String> parseCsvRow(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (quoted && current == '\"') {
                if (index + 1 < line.length() && line.charAt(index + 1) == '\"') {
                    value.append('\"');
                    index++;
                } else {
                    quoted = false;
                }
                continue;
            }
            if (quoted) {
                value.append(current);
                continue;
            }
            if (current == '\"' && value.isEmpty()) {
                quoted = true;
                continue;
            }
            if (current == ',') {
                columns.add(value.toString());
                value.setLength(0);
                continue;
            }
            value.append(current);
        }
        if (quoted) {
            throw new IllegalArgumentException("Quoted CSV value is not closed: " + line);
        }
        columns.add(value.toString());
        if (columns.size() < 6) {
            throw new IllegalArgumentException("CSV columns are missing: " + line);
        }
        return columns;
    }

    private Map<String, String> expectedAssignments(
        List<CuratedAssignment> assignments
    ) {
        Map<String, String> expected = new LinkedHashMap<>();
        for (CuratedAssignment assignment : assignments) {
            String previous = expected.put(
                assignment.researchFieldName(),
                assignment.categoryCode()
            );
            if (previous != null && !previous.equals(assignment.categoryCode())) {
                throw new IllegalArgumentException(
                    "Conflicting categories in CSV: " + assignment.researchFieldName()
                );
            }
        }
        return expected;
    }

    private Map<String, String> findAllMappings(Connection connection) throws SQLException {
        Map<String, String> mappings = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
            """
                SELECT field.name, category.code
                FROM research_field_category_mapping mapping
                JOIN research_field field
                  ON field.id = mapping.research_field_id
                JOIN research_field_category category
                  ON category.id = mapping.category_id
                ORDER BY field.name
                """
        ); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String fieldName = result.getString("name");
                String previous = mappings.put(fieldName, result.getString("code"));
                if (previous != null) {
                    throw new SQLException("Multiple categories found for field: " + fieldName);
                }
            }
        }
        return mappings;
    }

    private void insertFields(Connection connection, List<String> names) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO research_field (name) VALUES (?)"
        )) {
            for (String name : names) {
                statement.setString(1, name);
                statement.addBatch();
            }
            assertThat(statement.executeBatch()).hasSize(names.size());
        }
    }

    private Flyway flyway(String target) {
        FluentConfiguration configuration = Flyway.configure()
            .cleanDisabled(false)
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(MigrationVersion.fromVersion(target));
        }
        return configuration.load();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
            MYSQL.getJdbcUrl(),
            MYSQL.getUsername(),
            MYSQL.getPassword()
        );
    }

    private void assertMapping(
        Connection connection,
        String researchFieldName,
        String categoryCode
    ) throws SQLException {
        assertThat(count(
            connection,
            """
                SELECT COUNT(*)
                FROM research_field_category_mapping mapping
                JOIN research_field field
                  ON field.id = mapping.research_field_id
                JOIN research_field_category category
                  ON category.id = mapping.category_id
                WHERE field.name = ?
                  AND category.code = ?
                """,
            researchFieldName,
            categoryCode
        )).isOne();
    }

    private long findFieldId(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id FROM research_field WHERE name = ?"
        )) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getLong("id");
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

    private long count(
        Connection connection,
        String sql,
        Object... parameters
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
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

    private record CuratedAssignment(String researchFieldName, String categoryCode) {
    }
}
