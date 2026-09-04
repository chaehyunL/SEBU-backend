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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class ResearchFieldCategoryMySqlMigrationTest {
    private static final int EXPECTED_CATEGORY_COUNT = 21;
    private static final int EXPECTED_BASE_REFERENCE_COUNT = 535;
    private static final int EXPECTED_NATURAL_SCIENCE_REFERENCE_COUNT = 166;
    private static final String REFERENCE_ONLY_BASE_FIELD = "XAI 등";
    private static final String REFERENCE_ONLY_NATURAL_SCIENCE_FIELD = "NMR 분광학";
    private static final Path BASE_CLASSIFICATION_CSV = Path.of(
        "docs",
        "data",
        "research-field-category-classification.csv"
    );
    private static final Path NATURAL_SCIENCE_MAPPING_MIGRATION = Path.of(
        "src",
        "main",
        "resources",
        "db",
        "migration",
        "V33__seed_natural_science_research_field_category_mappings.sql"
    );
    private static final Pattern SQL_ASSIGNMENT_PATTERN = Pattern.compile(
        "^\\s*\\('(.+)', '([A-Z_]+)'\\)[,;]$"
    );

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @BeforeEach
    void cleanDatabase() {
        flyway(null).clean();
    }

    @Test
    void blankDatabaseCreatesCategoriesWithoutInventingResearchFields() throws Exception {
        flyway(null).migrate();

        try (Connection connection = connection()) {
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category"))
                .isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isZero();
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isZero();
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
    void v31UpgradeMapsOnlyExistingNaturalScienceFields() throws Exception {
        flyway("31").migrate();
        Map<String, String> naturalScienceAssignmentMap = expectedAssignments(
            readNaturalScienceAssignments()
        );
        assertThat(naturalScienceAssignmentMap)
            .hasSize(EXPECTED_NATURAL_SCIENCE_REFERENCE_COUNT)
            .containsEntry("이미지 처리", "SIGNAL_MEDIA")
            .containsEntry(REFERENCE_ONLY_NATURAL_SCIENCE_FIELD, "CHEMISTRY_MATERIALS");
        List<String> existingNaturalScienceFieldNames = naturalScienceAssignmentMap
            .keySet()
            .stream()
            .filter(name -> !name.equals(REFERENCE_ONLY_NATURAL_SCIENCE_FIELD))
            .toList();
        Map<String, String> expectedExistingAssignments = new LinkedHashMap<>(
            naturalScienceAssignmentMap
        );
        expectedExistingAssignments.remove(REFERENCE_ONLY_NATURAL_SCIENCE_FIELD);

        long existingImageProcessingId;
        long customFieldId;
        long fieldCountBeforeUpgrade;
        try (Connection connection = connection()) {
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isZero();
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isZero();
            insertFields(connection, existingNaturalScienceFieldNames);
            customFieldId = insertAndReturnId(
                connection,
                "INSERT INTO research_field (name) VALUES (?)",
                "V31 기존 사용자 정의 연구 분야"
            );
            existingImageProcessingId = findFieldId(connection, "이미지 처리");
            assertThat(executeUpdate(
                connection,
                """
                    INSERT INTO research_field_category_mapping (research_field_id, category_id)
                    SELECT field.id, category.id
                    FROM research_field field
                    JOIN research_field_category category
                      ON category.code = ?
                    WHERE field.name = ?
                    """,
                "SIGNAL_MEDIA",
                "이미지 처리"
            )).isOne();
            fieldCountBeforeUpgrade = count(connection, "SELECT COUNT(*) FROM research_field");
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category"))
                .isEqualTo(18L);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isOne();
        }

        flyway(null).migrate();

        try (Connection connection = connection()) {
            assertThat(findFieldId(connection, "이미지 처리"))
                .isEqualTo(existingImageProcessingId);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category"))
                .isEqualTo(EXPECTED_CATEGORY_COUNT);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isEqualTo(fieldCountBeforeUpgrade);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isEqualTo(existingNaturalScienceFieldNames.size());
            assertMapping(connection, "이미지 처리", "SIGNAL_MEDIA");
            assertMapping(connection, "가사이드 이론", "MATH_STATISTICS");
            assertMapping(connection, "우주론", "PHYSICS_ASTRONOMY");
            assertMapping(connection, "고분자 열역학", "CHEMISTRY_MATERIALS");
            assertThat(count(
                connection,
                "SELECT COUNT(*) FROM research_field WHERE name = ?",
                REFERENCE_ONLY_NATURAL_SCIENCE_FIELD
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
            assertThat(findAllMappings(connection))
                .isEqualTo(expectedExistingAssignments);
        }
    }

    @Test
    void v22UpgradeMapsExistingCuratedFieldsWithoutCreatingReferenceOnlyFields()
        throws Exception {
        flyway("22").migrate();
        List<CuratedAssignment> baseAssignments = readBaseAssignments();
        Map<String, String> baseAssignmentMap = expectedAssignments(baseAssignments);
        assertThat(baseAssignmentMap)
            .hasSize(EXPECTED_BASE_REFERENCE_COUNT)
            .containsEntry(REFERENCE_ONLY_BASE_FIELD, "AI_ML");
        List<String> baseFieldNames = baseAssignmentMap
            .keySet()
            .stream()
            .filter(name -> !name.equals(REFERENCE_ONLY_BASE_FIELD))
            .toList();
        assertThat(baseFieldNames)
            .hasSize(EXPECTED_BASE_REFERENCE_COUNT - 1)
            .doesNotHaveDuplicates();
        Map<String, String> expectedExistingAssignments = new LinkedHashMap<>(baseAssignmentMap);
        expectedExistingAssignments.remove(REFERENCE_ONLY_BASE_FIELD);

        long existingSeedFieldId;
        long customFieldId;
        long fieldCountBeforeUpgrade;
        try (Connection connection = connection()) {
            insertFields(connection, baseFieldNames);
            existingSeedFieldId = findFieldId(connection, "인공지능");
            customFieldId = insertAndReturnId(
                connection,
                "INSERT INTO research_field (name) VALUES (?)",
                "V22 기존 사용자 정의 연구 분야"
            );
            fieldCountBeforeUpgrade = count(connection, "SELECT COUNT(*) FROM research_field");
        }

        flyway(null).migrate();

        try (Connection connection = connection()) {
            assertThat(findFieldId(connection, "인공지능")).isEqualTo(existingSeedFieldId);
            assertThat(findFieldId(connection, "V22 기존 사용자 정의 연구 분야"))
                .isEqualTo(customFieldId);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field"))
                .isEqualTo(fieldCountBeforeUpgrade);
            assertThat(count(connection, "SELECT COUNT(*) FROM research_field_category_mapping"))
                .isEqualTo(baseFieldNames.size());
            assertThat(count(
                connection,
                "SELECT COUNT(DISTINCT research_field_id) FROM research_field_category_mapping"
            )).isEqualTo(baseFieldNames.size());
            assertThat(count(
                connection,
                """
                    SELECT COUNT(*)
                    FROM research_field_category category
                    LEFT JOIN research_field_category_mapping mapping
                      ON mapping.category_id = category.id
                    WHERE mapping.category_id IS NULL
                    """
            )).isEqualTo(3L);
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
            assertThat(count(
                connection,
                "SELECT COUNT(*) FROM research_field WHERE name = ?",
                REFERENCE_ONLY_BASE_FIELD
            )).isZero();
            assertThat(count(
                connection,
                "SELECT COUNT(*) FROM research_field WHERE name = ?",
                REFERENCE_ONLY_NATURAL_SCIENCE_FIELD
            )).isZero();
            assertThat(findAllMappings(connection))
                .isEqualTo(expectedExistingAssignments);
            assertThatThrownBy(() -> executeUpdate(
                connection,
                "DELETE FROM research_field_category WHERE code = ?",
                "AI_ML"
            )).isInstanceOf(SQLException.class);
        }
    }

    private List<CuratedAssignment> readBaseAssignments() throws Exception {
        return readCuratedAssignments(BASE_CLASSIFICATION_CSV, 0, 5);
    }

    private List<CuratedAssignment> readNaturalScienceAssignments() throws Exception {
        return Files.readAllLines(NATURAL_SCIENCE_MAPPING_MIGRATION, StandardCharsets.UTF_8)
            .stream()
            .map(SQL_ASSIGNMENT_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> new CuratedAssignment(matcher.group(1), matcher.group(2)))
            .toList();
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
