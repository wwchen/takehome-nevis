package com.nevis.search.enrichment

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import java.time.Duration

/**
 * One Claude call per document yields both the summary and the related search terms.
 * Any failure (network, rate limit, refusal, malformed reply) is logged and the [baseline]
 * enricher is used instead, so document creation never depends on the LLM being reachable.
 * Baseline thesaurus keywords are always merged in, so behaviour is a superset of offline mode.
 */
class ClaudeDocumentEnricher(
    apiKey: String,
    private val model: String,
    timeout: Duration,
    private val baseline: DocumentEnricher,
) : DocumentEnricher {

    private val log = LoggerFactory.getLogger(javaClass)
    private val json = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .timeout(timeout)
        .maxRetries(1)
        .build()

    override fun enrich(title: String, content: String): DocumentEnrichment {
        val fallback = baseline.enrich(title, content)
        val reply = try {
            call(title, content)
        } catch (e: Exception) {
            log.warn("Claude enrichment failed for '{}' ({}); using baseline", title, e.toString())
            return fallback
        } ?: return fallback

        val parsed = parse(reply) ?: run {
            log.warn("Claude enrichment reply for '{}' was not valid JSON; using baseline", title)
            return fallback
        }
        return DocumentEnrichment(
            summary = parsed.summary?.trim()?.ifEmpty { null } ?: fallback.summary,
            keywords = (parsed.keywords.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() } + fallback.keywords)
                .distinct()
                .take(ThesaurusKeywordExtractor.MAX_KEYWORDS),
        )
    }

    /** Returns the model's text reply, or null when it declined. */
    private fun call(title: String, content: String): String? {
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(MAX_OUTPUT_TOKENS)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            .system(SYSTEM_PROMPT)
            .addUserMessage("Title: $title\n\n$content")
            .build()
        val response = client.messages().create(params)
        if (response.stopReason().map { it == StopReason.REFUSAL }.orElse(false)) {
            log.warn("Claude declined to process document '{}'; using baseline", title)
            return null
        }
        return response.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("")
            .trim()
    }

    internal data class Reply(val summary: String? = null, val keywords: List<String>? = null)

    internal fun parse(text: String): Reply? {
        // Tolerate a stray code fence or leading prose around the JSON object.
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.readValue<Reply>(text.substring(start, end + 1)) }.getOrNull()
    }

    companion object {
        const val MAX_OUTPUT_TOKENS = 600L
        val SYSTEM_PROMPT = """
            You index documents belonging to wealth-management clients so their financial advisor can find them.
            Reply with a single JSON object and nothing else:
            {"summary": string, "keywords": [string, ...]}
            - summary: one or two plain sentences saying what the document is and its key facts (names, amounts, dates, addresses).
            - keywords: 5 to 15 short lower-case terms an advisor might type to find this document, beyond words already in it:
              document category, synonyms, and the purposes it serves (e.g. a utility bill is "proof of address").
        """.trimIndent()
    }
}
