package com.nevis.search.service

import com.nevis.search.api.ClientResponse
import com.nevis.search.api.ClientSearchResult
import com.nevis.search.api.DocumentResponse
import com.nevis.search.api.DocumentSearchResult
import com.nevis.search.api.SearchResult
import com.nevis.search.api.SearchType
import com.nevis.search.config.AppProperties
import com.nevis.search.embedding.Embedder
import com.nevis.search.repository.ClientRepository
import com.nevis.search.repository.DocumentRepository
import com.nevis.search.search.ClientMatcher
import com.nevis.search.search.DocumentMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToInt

@Service
class SearchService(
    private val clients: ClientRepository,
    private val documents: DocumentRepository,
    private val embedder: Embedder,
    private val props: AppProperties,
) {

    /**
     * @param query  free text, already validated as non-blank
     * @param type   restrict to one result type, or null for both
     * @param limit  max results, or null for the configured default
     */
    @Transactional(readOnly = true)
    fun search(query: String, type: SearchType?, limit: Int?): List<SearchResult> {
        val q = query.trim()
        val max = (limit ?: props.search.defaultLimit).coerceIn(1, props.search.maxLimit)

        val hits = buildList {
            if (type == null || type == SearchType.CLIENT) addAll(searchClients(q))
            if (type == null || type == SearchType.DOCUMENT) addAll(searchDocuments(q))
        }
        return rank(hits, max)
    }

    private fun searchClients(q: String): List<SearchResult> =
        clients.searchByText(ClientMatcher.toLikePattern(q)).mapNotNull { client ->
            ClientMatcher.match(q, client)?.let { m ->
                ClientSearchResult(score = m.score, matchedOn = m.matchedOn, client = ClientResponse.from(client))
            }
        }

    private fun searchDocuments(q: String): List<SearchResult> {
        val queryVector = embedder.embedQuery(q)
        return documents.findNearest(queryVector, props.search.documentCandidates).mapNotNull { hit ->
            DocumentMatcher.match(q, hit.document, hit.similarity, props.search.minDocumentSimilarity)?.let { m ->
                DocumentSearchResult(score = m.score, matchedOn = m.matchedOn, document = DocumentResponse.from(hit.document))
            }
        }
    }

    companion object {
        /** Highest score first; ties keep insertion order (clients before documents, then creation order). */
        fun rank(hits: List<SearchResult>, limit: Int): List<SearchResult> =
            hits.sortedByDescending { it.score }
                .take(limit)
                .map { it.withScore(round3(it.score)) }

        private fun round3(v: Double) = (v * 1000).roundToInt() / 1000.0

        private fun SearchResult.withScore(s: Double): SearchResult = when (this) {
            is ClientSearchResult -> copy(score = s)
            is DocumentSearchResult -> copy(score = s)
        }
    }
}
