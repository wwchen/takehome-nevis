package com.nevis.search.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class DocumentApiTest : IntegrationTestBase() {

    @Test
    fun `creates a document with summary and thesaurus keywords`() {
        val clientId = mvc.createClient()
        val result = mvc.postJson(
            "/clients/$clientId/documents",
            """{"title":"Utility bill – March 2026","content":"Electricity bill from Thames Power for 12 Harbour View. Amount due £142.18."}""",
        )
        assertThat(result.response.status).isEqualTo(201)
        val body = result.body()
        assertThat(result.response.getHeader("Location")).endsWith("/documents/" + body["id"].asString())
        assertThat(body["client_id"].asString()).isEqualTo(clientId)
        assertThat(body["title"].asString()).isEqualTo("Utility bill – March 2026")
        assertThat(body["summary"].asString()).startsWith("Electricity bill from Thames Power")
        assertThat(body["keywords"].items().map { it.asString() }).contains("proof of address")
        assertThat(body["created_at"].asString()).isNotBlank()

        val fetched = mvc.getJson("/documents/" + body["id"].asString())
        assertThat(fetched.response.status).isEqualTo(200)
        assertThat(fetched.body()["content"].asString()).contains("Thames Power")
    }

    @Test
    fun `lists a client's documents in creation order`() {
        val clientId = mvc.createClient()
        mvc.createDocument(clientId, "First", "one")
        mvc.createDocument(clientId, "Second", "two")
        val other = mvc.createClient(email = "other@example.com")
        mvc.createDocument(other, "Other", "three")

        val list = mvc.getJson("/clients/$clientId/documents").body()
        assertThat(list.items().map { it["title"].asString() }).containsExactly("First", "Second")
    }

    @Test
    fun `document for an unknown client is 404`() {
        val result = mvc.postJson("/clients/${UUID.randomUUID()}/documents", """{"title":"t","content":"c"}""")
        assertThat(result.response.status).isEqualTo(404)
        assertThat(mvc.getJson("/clients/${UUID.randomUUID()}/documents").response.status).isEqualTo(404)
    }

    @Test
    fun `blank title or content is 400`() {
        val clientId = mvc.createClient()
        val result = mvc.postJson("/clients/$clientId/documents", """{"title":" ","content":""}""")
        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.body()["errors"].has("title")).isTrue()
        assertThat(result.body()["errors"].has("content")).isTrue()
    }

    @Test
    fun `unknown document is 404`() {
        assertThat(mvc.getJson("/documents/${UUID.randomUUID()}").response.status).isEqualTo(404)
    }
}
