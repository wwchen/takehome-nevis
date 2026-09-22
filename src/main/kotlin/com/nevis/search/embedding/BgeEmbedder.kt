package com.nevis.search.embedding

import com.nevis.search.domain.EMBEDDING_DIMENSIONS
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * bge-small-en-v1.5 (quantized ONNX) running in-process via LangChain4j.
 * The model weights ship inside the dependency jar, so there is no download at runtime and no external service.
 * 384 dimensions, ~30 MB, a few ms per short text on CPU.
 */
@Component
class BgeEmbedder : Embedder {

    private val log = LoggerFactory.getLogger(javaClass)
    private val model: EmbeddingModel = BgeSmallEnV15QuantizedEmbeddingModel().also {
        log.info("Loaded in-process embedding model bge-small-en-v1.5 (quantized), {} dimensions", EMBEDDING_DIMENSIONS)
    }

    override val dimensions: Int = EMBEDDING_DIMENSIONS

    override fun embedDocument(text: String): FloatArray = model.embed(text).content().vector()

    /**
     * BGE recommends prefixing short retrieval queries with an instruction; passages are embedded as-is.
     * See https://huggingface.co/BAAI/bge-small-en-v1.5#frequently-asked-questions
     */
    override fun embedQuery(text: String): FloatArray = model.embed(QUERY_INSTRUCTION + text).content().vector()

    companion object {
        const val QUERY_INSTRUCTION = "Represent this sentence for searching relevant passages: "
    }
}
