package com.nevis.search.service

import com.nevis.search.api.ClientResponse
import com.nevis.search.api.ClientSearchResult
import com.nevis.search.api.DocumentResponse
import com.nevis.search.api.DocumentSearchResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class SearchServiceRankTest {

    private fun client(score: Double) = ClientSearchResult(
        score = score,
        matchedOn = listOf("name"),
        client = ClientResponse(UUID.randomUUID(), "A", "B", "a@b.io", null, emptyList(), OffsetDateTime.now()),
    )

    private fun document(score: Double) = DocumentSearchResult(
        score = score,
        matchedOn = listOf("semantic"),
        document = DocumentResponse(UUID.randomUUID(), UUID.randomUUID(), "t", "c", null, emptyList(), OffsetDateTime.now()),
    )

    @Test
    fun `orders by score descending across both types and applies the limit`() {
        val ranked = SearchService.rank(listOf(client(0.4), document(0.9), client(1.0), document(0.6)), limit = 3)
        assertThat(ranked.map { it.score }).containsExactly(1.0, 0.9, 0.6)
        assertThat(ranked.map { it.type }).containsExactly("client", "document", "document")
    }

    @Test
    fun `ties keep insertion order`() {
        val a = client(0.9)
        val b = document(0.9)
        assertThat(SearchService.rank(listOf(a, b), limit = 10).map { it.type }).containsExactly("client", "document")
    }

    @Test
    fun `scores are rounded to three decimals`() {
        assertThat(SearchService.rank(listOf(document(0.123456)), limit = 5).single().score).isEqualTo(0.123)
    }

    @Test
    fun `empty input gives empty output`() {
        assertThat(SearchService.rank(emptyList(), limit = 5)).isEmpty()
    }
}
