package com.nevis.search.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ClientApiTest : IntegrationTestBase() {

    @Test
    fun `creates a client and returns 201 with Location and snake_case body`() {
        val result = mvc.postJson(
            "/clients",
            """{"first_name":"John","last_name":"Doe","email":"john.doe@neviswealth.com",
                "description":"Retired surgeon","social_links":["https://linkedin.com/in/johndoe"]}""",
        )
        assertThat(result.response.status).isEqualTo(201)
        val body = result.body()
        assertThat(body["id"].asString()).isNotBlank()
        assertThat(result.response.getHeader("Location")).endsWith("/clients/" + body["id"].asString())
        assertThat(body["first_name"].asString()).isEqualTo("John")
        assertThat(body["email"].asString()).isEqualTo("john.doe@neviswealth.com")
        assertThat(body["social_links"][0].asString()).isEqualTo("https://linkedin.com/in/johndoe")
        assertThat(body["created_at"].asString()).isNotBlank()
        assertThat(body.has("firstName")).isFalse()

        val fetched = mvc.getJson("/clients/" + body["id"].asString())
        assertThat(fetched.response.status).isEqualTo(200)
        assertThat(fetched.body()["email"].asString()).isEqualTo("john.doe@neviswealth.com")
    }

    @Test
    fun `optional fields may be omitted`() {
        val result = mvc.postJson("/clients", """{"first_name":"Ann","last_name":"Lee","email":"ann@lee.io"}""")
        assertThat(result.response.status).isEqualTo(201)
        assertThat(result.body()["social_links"].isEmpty).isTrue()
        assertThat(result.body().has("description")).isFalse()
    }

    @Test
    fun `validation failures are 400 problem+json with per-field errors`() {
        val result = mvc.postJson("/clients", """{"first_name":"  ","email":"not-an-email"}""")
        assertThat(result.response.status).isEqualTo(400)
        assertThat(result.response.contentType).startsWith("application/problem+json")
        val errors = result.body()["errors"]
        assertThat(errors["first_name"].asString()).contains("blank")
        assertThat(errors["last_name"].asString()).contains("blank")
        assertThat(errors["email"].asString()).contains("email")
    }

    @Test
    fun `malformed JSON is 400`() {
        assertThat(mvc.postJson("/clients", "{oops").response.status).isEqualTo(400)
    }

    @Test
    fun `duplicate email is 409, case-insensitively`() {
        mvc.createClient(email = "dup@example.com")
        val result = mvc.postJson("/clients", """{"first_name":"X","last_name":"Y","email":"DUP@example.com"}""")
        assertThat(result.response.status).isEqualTo(409)
        assertThat(result.body()["detail"].asString()).contains("DUP@example.com")
    }

    @Test
    fun `unknown client is 404 and malformed id is 400`() {
        assertThat(mvc.getJson("/clients/${UUID.randomUUID()}").response.status).isEqualTo(404)
        assertThat(mvc.getJson("/clients/not-a-uuid").response.status).isEqualTo(400)
    }
}
