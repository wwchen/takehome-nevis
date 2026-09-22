package com.nevis.search.api

import com.nevis.search.service.SearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Search")
class SearchController(private val searchService: SearchService) {

    @GetMapping("/search")
    @Operation(
        summary = "Search clients and documents",
        description = """
            Returns a single list mixing clients and documents, ordered by relevance.

            * **Clients** match when the query appears (case-insensitively) in their name, email or description
              ("neviswealth" finds `john.doe@neviswealth.com`).
            * **Documents** match by meaning: the query is embedded and compared with each document's embedding,
              so "address proof" finds a document about a utility bill. A literal hit in the title or content
              is always kept and ranked at the top.
        """,
    )
    @ApiResponse(responseCode = "200", description = "OK (possibly empty list)")
    @ApiResponse(responseCode = "400", description = "Missing/blank `q` or invalid `type`/`limit`")
    fun search(
        @Parameter(description = "Free-text query", example = "address proof")
        @RequestParam @NotBlank @Size(max = 1000) q: String,
        @Parameter(description = "Restrict results to one type")
        @RequestParam(required = false) type: SearchType?,
        @Parameter(description = "Max results (default 20, max 100)")
        @RequestParam(required = false) @Min(1) @Max(100) limit: Int?,
    ): List<SearchResult> = searchService.search(q, type, limit)
}
