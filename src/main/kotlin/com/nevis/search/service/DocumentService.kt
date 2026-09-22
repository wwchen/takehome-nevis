package com.nevis.search.service

import com.nevis.search.api.ClientNotFoundException
import com.nevis.search.api.CreateDocumentRequest
import com.nevis.search.api.DocumentNotFoundException
import com.nevis.search.domain.Document
import com.nevis.search.embedding.Embedder
import com.nevis.search.enrichment.DocumentEnricher
import com.nevis.search.repository.ClientRepository
import com.nevis.search.repository.DocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DocumentService(
    private val clients: ClientRepository,
    private val documents: DocumentRepository,
    private val embedder: Embedder,
    private val enricher: DocumentEnricher,
) {

    /**
     * Keywords, summary and embedding are computed at write time so that search is a single DB query.
     * The (possibly slow) enrichment call happens outside any transaction.
     */
    fun create(clientId: UUID, req: CreateDocumentRequest): Document {
        val client = clients.findById(clientId).orElseThrow { ClientNotFoundException(clientId) }
        val title = req.title!!.trim()
        val content = req.content!!.trim()

        val enrichment = enricher.enrich(title, content)
        val embedding = embedder.embedDocument(embeddingText(title, content, enrichment.keywords))

        return documents.save(
            Document(
                client = client,
                title = title,
                content = content,
                summary = enrichment.summary,
                keywords = enrichment.keywords,
                embedding = embedding,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): Document = documents.findById(id).orElseThrow { DocumentNotFoundException(id) }

    @Transactional(readOnly = true)
    fun listForClient(clientId: UUID): List<Document> {
        if (!clients.existsById(clientId)) throw ClientNotFoundException(clientId)
        return documents.findAllByClientIdOrderByCreatedAtAsc(clientId)
    }

    companion object {
        /** Title and keywords carry a lot of signal for short documents, so they are embedded with the content. */
        fun embeddingText(title: String, content: String, keywords: List<String>): String =
            buildString {
                append(title).append("\n\n").append(content)
                if (keywords.isNotEmpty()) append("\n\nRelated: ").append(keywords.joinToString(", "))
            }
    }
}
