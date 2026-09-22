package com.nevis.search.search

import com.nevis.search.domain.Document
import com.nevis.search.enrichment.TextTokens

/**
 * Turns a raw cosine similarity into the document's search score and explains the match.
 *
 * Two signals are combined:
 *  1. **Lexical**: every token of the query occurs in the title, the keywords (related terms attached
 *     at write time, e.g. "proof of address" on a utility bill) or the content. A lexical hit is
 *     certain relevance, so its score is lifted to at least [LEXICAL_FLOOR].
 *  2. **Semantic**: cosine similarity between query and document embeddings, kept when it reaches the
 *     configured minimum. This catches paraphrases the thesaurus/keywords do not cover.
 */
object DocumentMatcher {

    data class Match(val score: Double, val matchedOn: List<String>)

    const val LEXICAL_FLOOR = 0.90

    fun match(query: String, document: Document, similarity: Double, minSimilarity: Double): Match? {
        val tokens = TextTokens.tokenSet(query)
        val matchedOn = mutableListOf<String>()
        if (TextTokens.containsAllTokens(document.title, tokens)) matchedOn += "title"
        if (TextTokens.containsAllTokens(document.keywords.joinToString("\n"), tokens)) matchedOn += "keywords"
        if (TextTokens.containsAllTokens(document.content, tokens)) matchedOn += "content"

        val lexical = matchedOn.isNotEmpty()
        if (!lexical && similarity < minSimilarity) return null

        val score = if (lexical) maxOf(similarity, LEXICAL_FLOOR) else similarity
        if (!lexical) matchedOn += "semantic"
        return Match(score = score.coerceIn(0.0, 1.0), matchedOn = matchedOn)
    }
}
