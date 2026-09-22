package com.nevis.search.api

import com.nevis.search.service.ClientService
import com.nevis.search.service.DocumentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("/clients")
@Tag(name = "Clients")
class ClientController(
    private val clientService: ClientService,
    private val documentService: DocumentService,
) {

    @PostMapping
    @Operation(summary = "Create a client")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    fun create(@Valid @RequestBody body: CreateClientRequest): ResponseEntity<ClientResponse> {
        val client = clientService.create(body)
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(client.id).toUri()
        return ResponseEntity.created(location).body(ClientResponse.from(client))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a client by id")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Client not found")
    fun get(@PathVariable id: UUID): ClientResponse = ClientResponse.from(clientService.get(id))

    @PostMapping("/{id}/documents")
    @Operation(
        summary = "Add a document to a client",
        description = "The content is embedded for semantic search and summarised at write time.",
    )
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Client not found")
    fun createDocument(
        @PathVariable id: UUID,
        @Valid @RequestBody body: CreateDocumentRequest,
    ): ResponseEntity<DocumentResponse> {
        val doc = documentService.create(id, body)
        val location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/documents/{id}").buildAndExpand(doc.id).toUri()
        return ResponseEntity.created(location).body(DocumentResponse.from(doc))
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "List a client's documents")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Client not found")
    fun listDocuments(@PathVariable id: UUID): List<DocumentResponse> =
        documentService.listForClient(id).map(DocumentResponse::from)
}
