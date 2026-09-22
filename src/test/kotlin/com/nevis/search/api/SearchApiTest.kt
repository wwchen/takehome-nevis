package com.nevis.search.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class SearchApiTest : IntegrationTestBase() {

    private lateinit var johnId: String
    private lateinit var mariaId: String

    @BeforeEach
    fun seed() {
        johnId = mvc.createClient("John", "Doe", "john.doe@neviswealth.com", "Retired surgeon, conservative risk profile.")
        mariaId = mvc.createClient("Maria", "Schmidt", "maria.schmidt@example.org", "Tech founder, aggressive growth.")

        mvc.createDocument(
            johnId, "Utility bill – March 2026",
            "Electricity bill from Thames Power for 12 Harbour View, London SE1 2AB, covering March 2026. Amount due £142.18.",
        )
        mvc.createDocument(
            johnId, "Passport scan",
            "Scanned copy of United Kingdom passport for John Doe, issued 12 May 2019, expires 12 May 2029. Used for KYC.",
        )
        mvc.createDocument(
            johnId, "Q1 2026 portfolio review",
            "Quarterly performance review of the balanced portfolio. Equities returned 4.2%, fixed income 1.1%.",
        )
        mvc.createDocument(
            mariaId, "Term sheet – Series A",
            "Term sheet for the Series A financing of Schmidt Robotics GmbH. Pre-money valuation EUR 18M.",
        )
        mvc.createDocument(
            mariaId, "Holiday photos",
            "A few pictures from our trip to Lisbon last summer. Great weather, lots of pastel de nata.",
        )
    }

    private fun JsonNode.titles() = items().filter { it["type"].asString() == "document" }.map { it["document"]["title"].asString() }
    private fun JsonNode.emails() = items().filter { it["type"].asString() == "client" }.map { it["client"]["email"].asString() }

    @Test
    fun `assignment example 1 - NevisWealth finds the client by email domain`() {
        val results = mvc.search("NevisWealth", type = "client")
        assertThat(results.emails()).containsExactly("john.doe@neviswealth.com")
        val hit = results[0]
        assertThat(hit["type"].asString()).isEqualTo("client")
        assertThat(hit["matched_on"].items().map { it.asString() }).containsExactly("email")
        assertThat(hit["score"].asDouble()).isBetween(0.0, 1.0)
    }

    @Test
    fun `assignment example 2 - address proof finds the utility bill`() {
        val results = mvc.search("address proof", type = "document")
        assertThat(results.titles()).startsWith("Utility bill – March 2026")
        assertThat(results.titles()).doesNotContain("Holiday photos", "Term sheet – Series A")
        assertThat(results[0]["matched_on"].items().map { it.asString() }).contains("keywords")
        assertThat(results[0]["document"]["summary"].asString()).isNotBlank()
    }

    @Test
    fun `literal terms in content rank first`() {
        val results = mvc.search("Lisbon trip", type = "document")
        assertThat(results.titles().first()).isEqualTo("Holiday photos")
        assertThat(results[0]["matched_on"].items().map { it.asString() }).contains("content")
    }

    @Test
    fun `clients match on name and description, documents on identity synonyms`() {
        assertThat(mvc.search("schmidt", type = "client").emails()).containsExactly("maria.schmidt@example.org")
        assertThat(mvc.search("surgeon", type = "client").emails()).containsExactly("john.doe@neviswealth.com")
        assertThat(mvc.search("identity document", type = "document").titles().first()).isEqualTo("Passport scan")
    }

    @Test
    fun `mixed results are ordered by score and typed`() {
        val results = mvc.search("john")
        assertThat(results.size()).isGreaterThan(1)
        assertThat(results[0]["type"].asString()).isEqualTo("client")
        val scores = results.items().map { it["score"].asDouble() }
        assertThat(scores).isSortedAccordingTo(Comparator.reverseOrder())
        results.items().forEach { r ->
            val t = r["type"].asString()
            assertThat(t).isIn("client", "document")
            assertThat(r.has(t)).isTrue()
        }
    }

    @Test
    fun `type filter and limit are honoured, type is case-insensitive`() {
        assertThat(mvc.search("john", type = "client").items().map { it["type"].asString() }).containsOnly("client")
        assertThat(mvc.search("john", type = "DOCUMENT").items().map { it["type"].asString() }).containsOnly("document")
        assertThat(mvc.search("john", limit = 2).size()).isEqualTo(2)
    }

    @Test
    fun `unrelated query returns an empty list`() {
        assertThat(mvc.search("zzqx quantum chromodynamics")).isEmpty()
    }

    @Test
    fun `search is case-insensitive and tolerant of surrounding whitespace`() {
        assertThat(mvc.search("  NEVISWEALTH  ", type = "client").emails()).containsExactly("john.doe@neviswealth.com")
    }

    @Test
    fun `invalid parameters are 400`() {
        assertThat(mvc.getWithParams("/search").response.status).isEqualTo(400)
        assertThat(mvc.getWithParams("/search", "q" to "   ").response.status).isEqualTo(400)
        assertThat(mvc.getWithParams("/search", "q" to "x", "type" to "people").response.status).isEqualTo(400)
        assertThat(mvc.getWithParams("/search", "q" to "x", "limit" to "0").response.status).isEqualTo(400)
        assertThat(mvc.getWithParams("/search", "q" to "x", "limit" to "101").response.status).isEqualTo(400)
        assertThat(mvc.getWithParams("/search", "q" to "x", "limit" to "ten").response.status).isEqualTo(400)
    }

    @Test
    fun `LIKE wildcards in the query are treated literally`() {
        assertThat(mvc.search("%", type = "client")).isEmpty()
        assertThat(mvc.search("_", type = "client")).isEmpty()
    }
}
