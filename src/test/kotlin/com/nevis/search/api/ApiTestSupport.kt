package com.nevis.search.api

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

val json: JsonMapper = JsonMapper.builder().build()

fun MvcResult.body(): JsonNode = json.readTree(response.contentAsString)

/**
 * Elements of an array node as a Kotlin list. Needed because Jackson 3's `JsonNode.map(Function)`
 * (which maps the node itself) shadows Kotlin's `Iterable.map`.
 */
fun JsonNode.items(): List<JsonNode> = toList()

fun MockMvc.postJson(path: String, body: String, vararg headers: Pair<String, String>): MvcResult =
    perform(
        post(path).contentType(MediaType.APPLICATION_JSON).content(body).apply {
            headers.forEach { (k, v) -> header(k, v) }
        },
    ).andReturn()

fun MockMvc.getJson(path: String, vararg headers: Pair<String, String>): MvcResult =
    perform(get(path).accept(MediaType.APPLICATION_JSON).apply { headers.forEach { (k, v) -> header(k, v) } }).andReturn()

fun MockMvc.createClient(
    first: String = "John",
    last: String = "Doe",
    email: String = "john.doe@neviswealth.com",
    description: String? = null,
): String {
    val desc = description?.let { ""","description":${json.writeValueAsString(it)}""" } ?: ""
    val result = postJson("/clients", """{"first_name":"$first","last_name":"$last","email":"$email"$desc}""")
    check(result.response.status == 201) { "client creation failed: ${result.response.contentAsString}" }
    return result.body()["id"].asString()
}

fun MockMvc.createDocument(clientId: String, title: String, content: String): JsonNode {
    val result = postJson(
        "/clients/$clientId/documents",
        """{"title":${json.writeValueAsString(title)},"content":${json.writeValueAsString(content)}}""",
    )
    check(result.response.status == 201) { "document creation failed: ${result.response.contentAsString}" }
    return result.body()
}

/** GET with query parameters passed as typed params, so encoding matches a real servlet container. */
fun MockMvc.getWithParams(path: String, vararg params: Pair<String, String>): MvcResult =
    perform(get(path).accept(MediaType.APPLICATION_JSON).apply { params.forEach { (k, v) -> param(k, v) } }).andReturn()

fun MockMvc.search(q: String, type: String? = null, limit: Int? = null): JsonNode {
    val params = buildList {
        add("q" to q)
        type?.let { add("type" to it) }
        limit?.let { add("limit" to it.toString()) }
    }
    val result = getWithParams("/search", *params.toTypedArray())
    check(result.response.status == 200) { "search failed: ${result.response.contentAsString}" }
    return result.body()
}
