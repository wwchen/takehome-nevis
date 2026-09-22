package com.nevis.search.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExtractiveSummarizerTest {

    private val summarizer = ExtractiveSummarizer(maxChars = 60)

    @Test
    fun `short content is returned whole with whitespace normalised`() {
        assertThat(summarizer.summarize("  Hello\n\n  world.  ")).isEqualTo("Hello world.")
    }

    @Test
    fun `long content is cut at a sentence boundary`() {
        val text = "First sentence here. Second sentence is here. Third sentence is quite a bit longer than the rest."
        assertThat(summarizer.summarize(text)).isEqualTo("First sentence here. Second sentence is here.")
    }

    @Test
    fun `a single over-long sentence is cut at a word boundary with an ellipsis`() {
        val text = "word ".repeat(40).trim()
        val summary = summarizer.summarize(text)!!
        assertThat(summary).endsWith("…")
        assertThat(summary.length).isLessThanOrEqualTo(61)
        assertThat(summary).doesNotContain("wor…")
    }

    @Test
    fun `blank content has no summary`() {
        assertThat(summarizer.summarize("   \n ")).isNull()
    }
}
