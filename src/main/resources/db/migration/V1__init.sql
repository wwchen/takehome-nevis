CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE clients (
    id           UUID PRIMARY KEY,
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    description  TEXT,
    social_links TEXT[]      NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Emails are unique case-insensitively.
CREATE UNIQUE INDEX uq_clients_email_lower ON clients (lower(email));

-- Trigram index so ILIKE '%term%' over the searchable client text stays fast as the table grows.
CREATE INDEX idx_clients_search_trgm ON clients
    USING gin ((first_name || ' ' || last_name || ' ' || email || ' ' || coalesce(description, '')) gin_trgm_ops);

CREATE TABLE documents (
    id         UUID PRIMARY KEY,
    client_id  UUID         NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    summary    TEXT,
    keywords   TEXT[]       NOT NULL DEFAULT '{}',
    embedding  VECTOR(384)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_client_id ON documents (client_id);

-- Approximate nearest-neighbour index for cosine similarity search.
CREATE INDEX idx_documents_embedding_hnsw ON documents USING hnsw (embedding vector_cosine_ops);
