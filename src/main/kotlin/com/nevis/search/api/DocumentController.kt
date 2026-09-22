package com.nevis.search.api

import com.nevis.search.service.DocumentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/documents")
@Tag(name = "Documents")
class DocumentController(private val documentService: DocumentService) {

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by id")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Document not found")
    fun get(@PathVariable id: UUID): DocumentResponse = DocumentResponse.from(documentService.get(id))
}
