package com.nevis.search.api

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Boots the whole application (real Flyway migrations, real pgvector queries, real embedding model)
 * against a PostgreSQL that is either
 *  - started by Testcontainers (`pgvector/pgvector:pg16`, needs Docker), or
 *  - the one pointed to by `TEST_DATABASE_URL` / `TEST_DATABASE_USER` / `TEST_DATABASE_PASSWORD`
 *    (for environments without Docker; the schema is rebuilt if it is out of date).
 * Document enrichment is forced to the offline baseline so no network is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestFlywayConfig::class)
@TestPropertySource(
    properties = [
        "app.enrichment.provider=baseline",
        "app.security.api-key=",
        "spring.flyway.clean-disabled=false",
    ],
)
abstract class IntegrationTestBase {

    @Autowired
    protected lateinit var mvc: MockMvc

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun cleanTables() {
        jdbc.execute("TRUNCATE TABLE documents, clients")
    }

    companion object {
        private val externalUrl: String? = System.getenv("TEST_DATABASE_URL")

        private val container: PostgreSQLContainer? =
            if (externalUrl == null) PostgreSQLContainer("pgvector/pgvector:pg16").also { it.start() } else null

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            if (container != null) {
                registry.add("spring.datasource.url") { container.jdbcUrl }
                registry.add("spring.datasource.username") { container.username }
                registry.add("spring.datasource.password") { container.password }
            } else {
                registry.add("spring.datasource.url") { externalUrl!! }
                registry.add("spring.datasource.username") { System.getenv("TEST_DATABASE_USER") ?: "nevis" }
                registry.add("spring.datasource.password") { System.getenv("TEST_DATABASE_PASSWORD") ?: "nevis" }
            }
        }
    }
}
