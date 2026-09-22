package com.nevis.search.embedding

/** Turns text into a fixed-size vector. Documents and queries may be encoded differently by the same model. */
interface Embedder {
    val dimensions: Int
    fun embedDocument(text: String): FloatArray
    fun embedQuery(text: String): FloatArray
}
