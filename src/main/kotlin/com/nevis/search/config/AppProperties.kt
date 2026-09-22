package com.nevis.search.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val search: Search = Search(),
    val enrichment: Enrichment = Enrichment(),
    val security: Security = Security(),
) {
    data class Search(
        /** Result count when the caller does not pass `limit`. */
        val defaultLimit: Int = 20,
        /** Hard cap on `limit`. */
        val maxLimit: Int = 100,
        /** How many nearest-neighbour documents to pull from pgvector before filtering and merging. */
        val documentCandidates: Int = 50,
        /** Documents whose cosine similarity to the query is below this are dropped. */
        val minDocumentSimilarity: Double = 0.55,
    )

    data class Enrichment(
        /** `auto` (Claude when a key is configured, otherwise baseline), `anthropic`, `baseline` or `none`. */
        val provider: String = "auto",
        val anthropic: Anthropic = Anthropic(),
        /** Length cap for the extractive (fallback) summary. */
        val summaryMaxChars: Int = 280,
    ) {
        data class Anthropic(
            val apiKey: String = "",
            val model: String = "claude-opus-5",
            val timeoutSeconds: Long = 30,
        )
    }

    data class Security(
        /** When non-blank every request (except docs and health) must carry this value in the `X-API-Key` header. */
        val apiKey: String = "",
    )
}
