package com.nevis.search.search

import com.nevis.search.domain.Client
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class ClientMatcherTest {

    private val john = Client(
        firstName = "John",
        lastName = "Doe",
        email = "john.doe@neviswealth.com",
        description = "Retired surgeon, conservative risk profile.",
    )

    @Test
    fun `assignment example - company name in the email domain matches the client`() {
        val m = ClientMatcher.match("NevisWealth", john)!!
        assertThat(m.matchedOn).containsExactly("email")
        // scored against the domain "neviswealth.com" (11/15), not the whole address
        assertThat(m.score).isCloseTo(11.0 / 15.0, within(1e-9))
    }

    @Test
    fun `exact first name scores 1 and reports name (and email, which contains it too)`() {
        val m = ClientMatcher.match("john", john)!!
        assertThat(m.score).isEqualTo(1.0)
        assertThat(m.matchedOn).containsExactly("name", "email")
    }

    @Test
    fun `full name with a space matches across first and last name`() {
        val m = ClientMatcher.match("John Doe", john)!!
        assertThat(m.score).isEqualTo(1.0)
        assertThat(m.matchedOn).containsExactly("name")
    }

    @Test
    fun `description matches with a low but non-zero score`() {
        val m = ClientMatcher.match("surgeon", john)!!
        assertThat(m.matchedOn).containsExactly("description")
        assertThat(m.score).isGreaterThanOrEqualTo(ClientMatcher.MIN_SCORE).isLessThan(0.5)
    }

    @Test
    fun `no substring anywhere means no match`() {
        assertThat(ClientMatcher.match("maria", john)).isNull()
        assertThat(ClientMatcher.match("   ", john)).isNull()
    }

    @Test
    fun `client without description still matches on other fields`() {
        val c = Client(firstName = "Ann", lastName = "Lee", email = "ann@x.io", description = null)
        assertThat(ClientMatcher.match("lee", c)?.matchedOn).containsExactly("name")
    }

    @Test
    fun `LIKE pattern escapes wildcards and is lower-cased`() {
        assertThat(ClientMatcher.toLikePattern("50%_Off!")).isEqualTo("%50!%!_off!!%")
    }
}
