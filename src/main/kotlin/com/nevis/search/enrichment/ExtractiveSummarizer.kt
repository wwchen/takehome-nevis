package com.nevis.search.enrichment

/**
 * Dependency-free summary: the leading sentences of the content, cut at a sentence boundary
 * where possible and capped at [maxChars].
 */
class ExtractiveSummarizer(private val maxChars: Int = 280) {

    fun summarize(content: String): String? {
        val text = content.replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return null
        if (text.length <= maxChars) return text

        val out = StringBuilder()
        for (s in SENTENCE_END.split(text)) {
            if (out.length + (if (out.isEmpty()) 0 else 1) + s.length > maxChars) break
            if (out.isNotEmpty()) out.append(' ')
            out.append(s)
        }
        if (out.isNotEmpty()) return out.toString()

        // The first sentence alone is too long: hard cut at a word boundary.
        val cut = text.take(maxChars)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > maxChars / 2) cut.substring(0, lastSpace) else cut).trimEnd() + "…"
    }

    private companion object {
        val SENTENCE_END = Regex("(?<=[.!?])\\s+")
    }
}
