package com.nevis.search.enrichment

import com.nevis.search.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class EnrichmentConfig(private val props: AppProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun thesaurusKeywordExtractor(): ThesaurusKeywordExtractor = ThesaurusKeywordExtractor.fromClasspath()

    @Bean
    fun documentEnricher(thesaurus: ThesaurusKeywordExtractor): DocumentEnricher {
        val e = props.enrichment
        val baseline = BaselineDocumentEnricher(thesaurus, ExtractiveSummarizer(e.summaryMaxChars))
        val hasKey = e.anthropic.apiKey.isNotBlank()

        return when (e.provider.lowercase()) {
            "none" -> NoopDocumentEnricher.also { log.info("Document enrichment disabled") }
            "baseline" -> baseline.also { log.info("Document enrichment: thesaurus keywords + extractive summary") }
            "anthropic" -> {
                require(hasKey) { "app.enrichment.provider=anthropic but no API key configured (ANTHROPIC_API_KEY)" }
                claude(e, baseline)
            }
            "auto" -> if (hasKey) claude(e, baseline) else baseline.also {
                log.info("Document enrichment: thesaurus keywords + extractive summary (set ANTHROPIC_API_KEY to use Claude)")
            }
            else -> throw IllegalArgumentException("Unknown app.enrichment.provider '${e.provider}'")
        }
    }

    private fun claude(e: AppProperties.Enrichment, baseline: DocumentEnricher): DocumentEnricher {
        log.info("Document enrichment: Claude ({}) with baseline fallback", e.anthropic.model)
        return ClaudeDocumentEnricher(
            apiKey = e.anthropic.apiKey,
            model = e.anthropic.model,
            timeout = Duration.ofSeconds(e.anthropic.timeoutSeconds),
            baseline = baseline,
        )
    }
}
