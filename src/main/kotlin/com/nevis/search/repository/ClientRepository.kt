package com.nevis.search.repository

import com.nevis.search.domain.Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ClientRepository : JpaRepository<Client, UUID> {

    fun existsByEmailIgnoreCase(email: String): Boolean

    /**
     * Case-insensitive substring match over every searchable client field.
     * [pattern] must already be lower-cased, LIKE-escaped and wrapped in `%`.
     */
    @Query(
        """
        select c from Client c
        where lower(c.firstName) like :pattern escape '!'
           or lower(c.lastName) like :pattern escape '!'
           or lower(concat(c.firstName, ' ', c.lastName)) like :pattern escape '!'
           or lower(c.email) like :pattern escape '!'
           or lower(coalesce(c.description, '')) like :pattern escape '!'
        order by c.createdAt asc
        """,
    )
    fun searchByText(@Param("pattern") pattern: String): List<Client>
}
