package com.sebu.backend.auth.repository;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class AuthenticationMySqlMigrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Test
    void upgradesExistingV12DataThroughLatestAuthenticationSchemaOnMySql8() throws Exception {
        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion("12"))
            .load()
            .migrate();

        try (var connection = DriverManager.getConnection(
            MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()
        ); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO app_user (email) VALUES ('legacy@example.com')");
        }

        Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .load()
            .migrate();

        try (var connection = DriverManager.getConnection(
            MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()
        ); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("""
                SELECT email, provider, provider_user_id, profile_completed, sejong_department_name
                FROM app_user
                WHERE email = 'legacy@example.com'
                """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("email")).isEqualTo("legacy@example.com");
                assertThat(result.getString("provider")).isNull();
                assertThat(result.getString("provider_user_id")).isNull();
                assertThat(result.getBoolean("profile_completed")).isFalse();
                assertThat(result.getString("sejong_department_name")).isNull();
            }

            statement.executeUpdate("""
                INSERT INTO app_user (provider, provider_user_id, profile_completed)
                VALUES ('SEJONG', '21012345', FALSE)
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO app_user (provider, provider_user_id, profile_completed)
                VALUES ('SEJONG', '21012345', FALSE)
                """))
                .isInstanceOf(SQLException.class);
        }
    }
}
