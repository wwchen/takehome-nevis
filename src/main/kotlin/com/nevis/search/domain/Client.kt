package com.nevis.search.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "clients")
class Client(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(nullable = false)
    var email: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "social_links", nullable = false, columnDefinition = "text[]")
    var socialLinks: List<String> = emptyList(),

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    val fullName: String get() = "$firstName $lastName"
}
