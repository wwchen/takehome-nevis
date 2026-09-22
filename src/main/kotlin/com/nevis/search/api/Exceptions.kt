package com.nevis.search.api

import java.util.UUID

class ClientNotFoundException(id: UUID) : RuntimeException("Client $id not found")

class DocumentNotFoundException(id: UUID) : RuntimeException("Document $id not found")

class DuplicateEmailException(email: String) : RuntimeException("A client with email '$email' already exists")
