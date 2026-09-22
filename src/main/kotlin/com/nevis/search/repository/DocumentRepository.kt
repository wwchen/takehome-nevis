package com.nevis.search.repository

import com.nevis.search.domain.Document
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Tuple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/** A document together with its cosine similarity (1 = identical, 0 = orthogonal) to a query vector. */
data class DocumentHit(val document: Document, val similarity: Double)

interface DocumentVectorSearch {
    /** Nearest neighbours of [queryEmbedding] by cosine distance, best first. */
    fun findNearest(queryEmbedding: FloatArray, limit: Int): List<DocumentHit>
}

@Repository
class DocumentVectorSearchImpl(
    @PersistenceContext private val em: EntityManager,
) : DocumentVectorSearch {

    override fun findNearest(queryEmbedding: FloatArray, limit: Int): List<DocumentHit> =
        em.createQuery(
            """
            select d as doc, cosine_distance(d.embedding, :q) as dist
            from Document d
            order by dist asc, d.createdAt asc
            """,
            Tuple::class.java,
        )
            .setParameter("q", queryEmbedding)
            .setMaxResults(limit)
            .resultList
            .map { row ->
                val distance = (row.get("dist") as Number).toDouble()
                DocumentHit(document = row.get("doc") as Document, similarity = 1.0 - distance)
            }
}

interface DocumentRepository : JpaRepository<Document, UUID>, DocumentVectorSearch {
    fun findAllByClientIdOrderByCreatedAtAsc(clientId: UUID): List<Document>
}
