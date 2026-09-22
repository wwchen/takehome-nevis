package com.nevis.search.search

import com.nevis.search.domain.Client

/**
 * Lexical client matching: case-insensitive substring over name, email and description.
 *
 * The score is "how much of the matched field the query covers" (query length / field length),
 * so `john` against first name `John` scores 1.0 while `NevisWealth` against
 * `john.doe@neviswealth.com` scores against the best sub-part of the email (the domain), not the whole string.
 */
object ClientMatcher {

    data class Match(val score: Double, val matchedOn: List<String>)

    const val MIN_SCORE = 0.05

    fun match(query: String, client: Client): Match? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null

        val candidates = listOf(
            "name" to listOf(client.firstName, client.lastName, client.fullName),
            "email" to emailParts(client.email),
            "description" to listOfNotNull(client.description),
        )

        var best = 0.0
        val matched = mutableListOf<String>()
        for ((field, values) in candidates) {
            val coverage = values
                .map { it.lowercase() }
                .filter { it.contains(q) }
                .maxOfOrNull { q.length.toDouble() / it.length }
                ?: continue
            matched += field
            if (coverage > best) best = coverage
        }
        if (matched.isEmpty()) return null
        return Match(score = best.coerceIn(MIN_SCORE, 1.0), matchedOn = matched)
    }

    /** Whole email, local part and domain, so a query can be scored against the part it actually hit. */
    private fun emailParts(email: String): List<String> {
        val at = email.indexOf('@')
        if (at <= 0 || at == email.lastIndex) return listOf(email)
        return listOf(email, email.substring(0, at), email.substring(at + 1))
    }

    /** Escapes LIKE metacharacters using `!` as the escape char and wraps the query in `%`. */
    fun toLikePattern(query: String): String {
        val escaped = query.trim().lowercase()
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
        return "%$escaped%"
    }
}
