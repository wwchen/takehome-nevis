package com.nevis.search.search

import com.nevis.search.domain.Client
import com.nevis.search.domain.Document
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DocumentMatcherTest {

    private val client = Client(firstName = "J", lastName = "D", email = "j@d.io")
    private val bill = Document(
        client = client,
        title = "Utility bill – March 2026",
        content = "Electricity bill from Thames Power for 12 Harbour View, London.",
        keywords = listOf("proof of address", "address proof", "council tax"),
        embedding = FloatArray(384),
    )

    @Test
    fun `keyword hit is kept and lifted to the lexical floor even with low similarity`() {
        val m = DocumentMatcher.match("address proof", bill, similarity = 0.30, minSimilarity = 0.55)!!
        assertThat(m.matchedOn).containsExactly("keywords")
        assertThat(m.score).isEqualTo(DocumentMatcher.LEXICAL_FLOOR)
    }

    @Test
    fun `title and content hits are both reported, floor does not lower a high similarity`() {
        val m = DocumentMatcher.match("Bill", bill, similarity = 0.95, minSimilarity = 0.55)!!
        assertThat(m.matchedOn).containsExactly("title", "content")
        assertThat(m.score).isEqualTo(0.95)
    }

    @Test
    fun `semantic-only hit passes when similarity reaches the minimum`() {
        val m = DocumentMatcher.match("where does he live", bill, similarity = 0.60, minSimilarity = 0.55)!!
        assertThat(m.matchedOn).containsExactly("semantic")
        assertThat(m.score).isEqualTo(0.60)
    }

    @Test
    fun `semantic-only hit below the minimum is dropped`() {
        assertThat(DocumentMatcher.match("pizza recipe", bill, similarity = 0.40, minSimilarity = 0.55)).isNull()
    }

    @Test
    fun `partial token overlap is not a lexical match`() {
        // "harbour" is in the content, "bank" is not
        val m = DocumentMatcher.match("harbour bank", bill, similarity = 0.40, minSimilarity = 0.55)
        assertThat(m).isNull()
    }

    @Test
    fun `score is clamped to the unit interval`() {
        assertThat(DocumentMatcher.match("x", bill, similarity = 1.2, minSimilarity = 0.0)!!.score).isEqualTo(1.0)
    }
}
