package com.nevis.search.enrichment

import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

/**
 * Offline "similar terms" support. The thesaurus is a list of concept groups
 * (see `thesaurus.json`). When a document mentions any term of a group, every other term of that
 * group becomes a keyword of the document, so a utility bill is findable as "proof of address".
 */
class ThesaurusKeywordExtractor(private val groups: List<List<String>>) {

    fun extract(text: String, maxKeywords: Int = MAX_KEYWORDS): List<String> {
        val out = LinkedHashSet<String>()
        for (group in groups) {
            val hits = group.filter { TextTokens.containsPhrase(text, it) }
            if (hits.isEmpty()) continue
            group.filterNot { it in hits }.forEach { out += it }
        }
        return out.take(maxKeywords)
    }

    companion object {
        const val MAX_KEYWORDS = 40
        const val RESOURCE = "thesaurus.json"

        fun fromClasspath(resource: String = RESOURCE): ThesaurusKeywordExtractor {
            val json = ClassPathResource(resource).inputStream.use { it.readBytes() }
            val groups: List<List<String>> = JsonMapper.builder().build().readValue(json)
            return ThesaurusKeywordExtractor(groups)
        }
    }
}
