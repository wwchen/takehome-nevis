package com.nevis.search.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TextTokensTest {

    @Test
    fun `lower-cases, drops stop words and punctuation, strips plurals`() {
        assertThat(TextTokens.tokenize("The Proofs of Address, please!"))
            .containsExactly("proof", "address", "please")
        assertThat(TextTokens.tokenize("policies")).containsExactly("policy")
        assertThat(TextTokens.tokenize("address")).containsExactly("address") // ends in "ss": not a plural
    }

    @Test
    fun `containsAllTokens ignores order and inflection`() {
        assertThat(TextTokens.containsAllTokens("Proof of Address", TextTokens.tokenSet("address proofs"))).isTrue()
        assertThat(TextTokens.containsAllTokens("Proof of Address", TextTokens.tokenSet("address proof bank"))).isFalse()
    }

    @Test
    fun `empty or stop-word-only query never matches`() {
        assertThat(TextTokens.containsAllTokens("anything", TextTokens.tokenSet(""))).isFalse()
        assertThat(TextTokens.containsAllTokens("anything", TextTokens.tokenSet("the of"))).isFalse()
    }

    @Test
    fun `containsPhrase requires a contiguous token sequence`() {
        assertThat(TextTokens.containsPhrase("Your electricity bill is attached", "electricity bill")).isTrue()
        assertThat(TextTokens.containsPhrase("Electricity: the bill is attached", "electricity bill")).isTrue() // stop word removed
        assertThat(TextTokens.containsPhrase("Electricity prices and the water bill", "electricity bill")).isFalse()
        assertThat(TextTokens.containsPhrase("short", "much longer phrase")).isFalse()
    }
}
