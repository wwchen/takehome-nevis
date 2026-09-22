package com.nevis.search.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

/** Only the offline parts of the Claude enricher are tested; no network call is made. */
class ClaudeDocumentEnricherTest {

    private val enricher = ClaudeDocumentEnricher(
        apiKey = "test-key",
        model = "claude-opus-5",
        timeout = Duration.ofSeconds(1),
        baseline = NoopDocumentEnricher,
    )

    @Test
    fun `parses a plain JSON reply`() {
        val reply = enricher.parse("""{"summary":"A bill.","keywords":["proof of address","utility bill"]}""")
        assertThat(reply?.summary).isEqualTo("A bill.")
        assertThat(reply?.keywords).containsExactly("proof of address", "utility bill")
    }

    @Test
    fun `tolerates code fences and prose around the JSON`() {
        val reply = enricher.parse("Sure! ```json\n{\"summary\": \"x\", \"keywords\": []}\n```")
        assertThat(reply?.summary).isEqualTo("x")
        assertThat(reply?.keywords).isEmpty()
    }

    @Test
    fun `unknown fields and missing fields are tolerated`() {
        val reply = enricher.parse("""{"summary":"x","extra":1}""")
        assertThat(reply?.summary).isEqualTo("x")
        assertThat(reply?.keywords).isNull()
    }

    @Test
    fun `garbage yields null`() {
        assertThat(enricher.parse("no json here")).isNull()
        assertThat(enricher.parse("{not json}")).isNull()
    }
}
