package com.nevis.search.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

/**
 * Minimal shared-secret auth so the API can be deployed publicly.
 * Disabled unless `app.security.api-key` (env `API_KEY`) is set.
 */
@Component
class ApiKeyFilter(
    private val props: AppProperties,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    companion object {
        const val HEADER = "X-API-Key"
        private val PUBLIC_PREFIXES = listOf("/swagger-ui", "/v3/api-docs", "/actuator/health", "/docs")
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (props.security.apiKey.isBlank()) return true
        val path = request.requestURI
        return PUBLIC_PREFIXES.any { path == it || path.startsWith("$it/") || path.startsWith("$it.") }
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val presented = request.getHeader(HEADER)
        if (presented != null && constantTimeEquals(presented, props.security.apiKey)) {
            chain.doFilter(request, response)
            return
        }
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.setHeader("WWW-Authenticate", "ApiKey realm=\"nevis-search\"")
        val body = mapOf(
            "type" to "about:blank",
            "title" to "Unauthorized",
            "status" to 401,
            "detail" to "Missing or invalid $HEADER header",
            "instance" to request.requestURI,
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        java.security.MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
