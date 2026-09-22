package com.nevis.search.api

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * Tests may run against a persistent database (`TEST_DATABASE_URL`) whose schema was created by an
 * older version of the migrations. Only when Flyway's validation fails is the schema dropped and
 * rebuilt; otherwise it is left alone, because several Spring test contexts share one database and
 * dropping tables under a live connection pool invalidates its cached prepared statements.
 */
@TestConfiguration
class TestFlywayConfig {
    @Bean
    fun cleanOnlyWhenInvalid() = FlywayMigrationStrategy { flyway ->
        if (!flyway.validateWithResult().validationSuccessful) {
            flyway.clean()
        }
        flyway.migrate()
    }
}
