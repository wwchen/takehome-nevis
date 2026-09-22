package com.nevis.search.enrichment

/**
 * Tiny tokenizer shared by keyword extraction and lexical matching:
 * lower-case, split on non-alphanumerics, drop stop words, strip a plural "s".
 * Deliberately simple; it only needs to make "address proof" and "Proof of Address" agree.
 */
object TextTokens {

    private val STOP_WORDS = setOf(
        "a", "an", "the", "of", "for", "to", "and", "or", "in", "on", "at", "by", "with", "from",
        "is", "are", "was", "be", "my", "our", "your", "their", "this", "that", "it", "its", "as",
    )

    private val SPLIT = Regex("[^\\p{L}\\p{N}]+")

    fun tokenize(text: String): List<String> =
        SPLIT.split(text.lowercase())
            .filter { it.isNotEmpty() && it !in STOP_WORDS }
            .map(::stem)

    /** Distinct tokens, order preserved. */
    fun tokenSet(text: String): Set<String> = tokenize(text).toSet()

    /** True when every token of [query] occurs somewhere in [text]. Empty query never matches. */
    fun containsAllTokens(text: String, query: Set<String>): Boolean =
        query.isNotEmpty() && tokenSet(text).containsAll(query)

    /** True when the tokens of [phrase] occur in [text] as a contiguous sequence. */
    fun containsPhrase(text: String, phrase: String): Boolean {
        val needle = tokenize(phrase)
        if (needle.isEmpty()) return false
        val hay = tokenize(text)
        if (needle.size > hay.size) return false
        return hay.windowed(needle.size).any { it == needle }
    }

    private fun stem(token: String): String = when {
        token.length > 4 && token.endsWith("ies") -> token.dropLast(3) + "y"
        token.length > 3 && token.endsWith("s") && !token.endsWith("ss") -> token.dropLast(1)
        else -> token
    }
}
