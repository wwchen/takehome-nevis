package com.nevis.search.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ThesaurusKeywordExtractorTest {

    private val extractor = ThesaurusKeywordExtractor(
        listOf(
            listOf("proof of address", "utility bill", "electricity bill", "council tax"),
            listOf("proof of identity", "passport", "driving licence"),
        ),
    )

    @Test
    fun `document mentioning one term of a group gets the other terms as keywords`() {
        val keywords = extractor.extract("Electricity bill for March from Thames Power")
        assertThat(keywords).containsExactly("proof of address", "utility bill", "council tax")
    }

    @Test
    fun `terms already present in the text are not repeated as keywords`() {
        val keywords = extractor.extract("Utility bill (electricity bill) for March")
        assertThat(keywords).containsExactly("proof of address", "council tax")
    }

    @Test
    fun `document matching no group has no keywords`() {
        assertThat(extractor.extract("Holiday photos from Lisbon")).isEmpty()
    }

    @Test
    fun `keywords are capped`() {
        assertThat(extractor.extract("passport and electricity bill", maxKeywords = 2)).hasSize(2)
    }

    @Test
    fun `bundled thesaurus loads and covers the assignment example`() {
        val bundled = ThesaurusKeywordExtractor.fromClasspath()
        assertThat(bundled.extract("Utility bill – electricity, March 2026")).contains("proof of address", "address proof")
    }
}
