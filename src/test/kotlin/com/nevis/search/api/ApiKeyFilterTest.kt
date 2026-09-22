package com.nevis.search.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["app.security.api-key=s3cret"])
class ApiKeyFilterTest : IntegrationTestBase() {

    @Test
    fun `requests without the key are rejected with 401 problem+json`() {
        val result = mvc.getJson("/search?q=john")
        assertThat(result.response.status).isEqualTo(401)
        assertThat(result.response.contentType).startsWith("application/problem+json")
        assertThat(result.body()["detail"].asString()).contains("X-API-Key")
    }

    @Test
    fun `wrong key is rejected, right key is accepted`() {
        assertThat(mvc.getJson("/search?q=john", "X-API-Key" to "nope").response.status).isEqualTo(401)
        assertThat(mvc.getJson("/search?q=john", "X-API-Key" to "s3cret").response.status).isEqualTo(200)
        assertThat(mvc.postJson("/clients", """{"first_name":"A","last_name":"B","email":"a@b.io"}""", "X-API-Key" to "s3cret").response.status).isEqualTo(201)
    }

    @Test
    fun `docs and health stay public`() {
        assertThat(mvc.getJson("/v3/api-docs").response.status).isEqualTo(200)
        assertThat(mvc.getJson("/actuator/health").response.status).isEqualTo(200)
    }
}
