package com.nevis.search.service

import com.nevis.search.api.ClientNotFoundException
import com.nevis.search.api.CreateClientRequest
import com.nevis.search.api.DuplicateEmailException
import com.nevis.search.domain.Client
import com.nevis.search.repository.ClientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ClientService(private val clients: ClientRepository) {

    @Transactional
    fun create(req: CreateClientRequest): Client {
        val email = req.email!!.trim()
        if (clients.existsByEmailIgnoreCase(email)) throw DuplicateEmailException(email)
        val client = Client(
            firstName = req.firstName!!.trim(),
            lastName = req.lastName!!.trim(),
            email = email,
            description = req.description?.trim()?.ifEmpty { null },
            socialLinks = req.socialLinks?.map { it.trim() } ?: emptyList(),
        )
        return clients.save(client)
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): Client = clients.findById(id).orElseThrow { ClientNotFoundException(id) }
}
