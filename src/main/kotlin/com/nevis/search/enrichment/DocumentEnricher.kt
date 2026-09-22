package com.nevis.search.enrichment

/** Metadata derived from a document at write time. */
data class DocumentEnrichment(
    /** Short human-readable summary, or null when unavailable. */
    val summary: String?,
    /** Related search terms: synonyms, categories, concepts an advisor might search for to find this document. */
    val keywords: List<String>,
) {
    companion object {
        val EMPTY = DocumentEnrichment(summary = null, keywords = emptyList())
    }
}

interface DocumentEnricher {
    fun enrich(title: String, content: String): DocumentEnrichment
}

/** Used when enrichment is switched off. */
object NoopDocumentEnricher : DocumentEnricher {
    override fun enrich(title: String, content: String) = DocumentEnrichment.EMPTY
}

/**
 * Dependency-free default: keywords from the built-in domain thesaurus and an extractive summary.
 * Deterministic, offline, and what the test suite runs against.
 */
class BaselineDocumentEnricher(
    private val thesaurus: ThesaurusKeywordExtractor,
    private val summarizer: ExtractiveSummarizer,
) : DocumentEnricher {
    override fun enrich(title: String, content: String) = DocumentEnrichment(
        summary = summarizer.summarize(content),
        keywords = thesaurus.extract("$title\n$content"),
    )
}
