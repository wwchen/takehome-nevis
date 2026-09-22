# Nevis Search API

[![CI](https://github.com/wwchen/takehome-nevis/actions/workflows/ci.yml/badge.svg)](https://github.com/wwchen/takehome-nevis/actions/workflows/ci.yml)

A small Kotlin / Spring Boot service that stores advisors' clients and their documents and exposes one
search endpoint across both:

* **Clients** are found by name, email or description: `NevisWealth` finds `john.doe@neviswealth.com`.
* **Documents** are found by meaning, not just words: `address proof` finds a document about a utility bill.
* Every document gets a **summary** and a set of **related search terms** when it is created
  (via Claude when an API key is configured, otherwise from a built-in domain thesaurus).

Stack: Kotlin 2.3, Spring Boot 4.1 (Web MVC, Data JPA, Validation), PostgreSQL 16 + pgvector, Flyway,
LangChain4j in-process embeddings (bge-small-en-v1.5, ONNX, bundled in the jar), the official Anthropic
Java SDK, springdoc-openapi, JUnit 5 + Testcontainers.

---

## Quick start

Requirements: Docker with Compose. Nothing else; the embedding model ships inside the image.

```bash
docker compose up --build
```

First build takes a few minutes (Gradle downloads dependencies). The API is then at
<http://localhost:8080> and interactive docs at **<http://localhost:8080/docs>**.

Optional environment (put in `.env`, see `.env.example`):

| Variable | Default | Effect |
|---|---|---|
| `ANTHROPIC_API_KEY` | empty | When set, Claude writes each document's summary and related search terms. Without it, the built-in thesaurus and an extractive summary are used. |
| `ANTHROPIC_MODEL` | `claude-opus-5` | Model used for the above. |
| `API_KEY` | empty | When set, every request must send `X-API-Key: <value>` (docs and `/actuator/health` stay public). Use this when deploying publicly. |
| `MIN_DOCUMENT_SIMILARITY` | `0.55` | Documents below this cosine similarity are dropped unless they match lexically. |
| `ENRICHMENT_PROVIDER` | `auto` | `auto` / `anthropic` / `baseline` / `none`. |

---

## Try it

```bash
# 1. Create a client
curl -s -X POST localhost:8080/clients -H 'Content-Type: application/json' -d '{
  "first_name": "John", "last_name": "Doe", "email": "john.doe@neviswealth.com",
  "description": "Retired surgeon, conservative risk profile, interested in ESG funds.",
  "social_links": ["https://linkedin.com/in/johndoe"]
}'
```
```json
{"id":"65a434b6-bad5-4294-aa72-1522fd16c959","first_name":"John","last_name":"Doe",
 "email":"john.doe@neviswealth.com","description":"Retired surgeon, conservative risk profile, interested in ESG funds.",
 "social_links":["https://linkedin.com/in/johndoe"],"created_at":"2026-09-22T18:33:47.364880205Z"}
```

```bash
# 2. Add a document to that client
curl -s -X POST localhost:8080/clients/65a434b6-bad5-4294-aa72-1522fd16c959/documents \
  -H 'Content-Type: application/json' -d '{
  "title": "Utility bill – March 2026",
  "content": "Electricity bill from Thames Power for 12 Harbour View, London SE1 2AB, covering 1–31 March 2026. Account holder John Doe. Amount due £142.18, payable by 21 April 2026."
}'
```
```json
{"id":"40baec60-db21-43ac-9c8b-cc0836ce88bd","client_id":"65a434b6-bad5-4294-aa72-1522fd16c959",
 "title":"Utility bill – March 2026",
 "content":"Electricity bill from Thames Power for 12 Harbour View, London SE1 2AB, covering 1–31 March 2026. Account holder John Doe. Amount due £142.18, payable by 21 April 2026.",
 "summary":"Electricity bill from Thames Power for 12 Harbour View, London SE1 2AB, covering 1–31 March 2026. Account holder John Doe. Amount due £142.18, payable by 21 April 2026.",
 "keywords":["proof of address","address proof","address verification","proof of residence","residential address","gas bill","water bill","phone bill","council tax","bank statement","tenancy agreement","lease agreement","invoice","receipt","payment confirmation","remittance"],
 "created_at":"2026-09-22T18:33:47.636742641Z"}
```
(With `ANTHROPIC_API_KEY` set, `summary` is a written summary and `keywords` also include Claude's suggestions.)

```bash
# 3. Search — clients by a term in their email
curl -s 'localhost:8080/search?q=NevisWealth'
```
```json
[
  {
    "type": "client",
    "score": 0.733,
    "matched_on": ["email"],
    "client": {
      "id": "65a434b6-bad5-4294-aa72-1522fd16c959",
      "first_name": "John", "last_name": "Doe", "email": "john.doe@neviswealth.com",
      "description": "Retired surgeon, conservative risk profile, interested in ESG funds.",
      "social_links": ["https://linkedin.com/in/johndoe"],
      "created_at": "2026-09-22T18:33:47.36488Z"
    }
  }
]
```

```bash
# 4. Search — documents by a related concept (the word "address proof" appears nowhere in the bill)
curl -s 'localhost:8080/search?q=address%20proof&type=document'
```
```json
[
  {
    "type": "document",
    "score": 0.9,
    "matched_on": ["keywords"],
    "document": {
      "id": "40baec60-db21-43ac-9c8b-cc0836ce88bd",
      "client_id": "65a434b6-bad5-4294-aa72-1522fd16c959",
      "title": "Utility bill – March 2026",
      "content": "Electricity bill from Thames Power for 12 Harbour View, London SE1 2AB, covering 1–31 March 2026. Account holder John Doe. Amount due £142.18, payable by 21 April 2026.",
      "summary": "Electricity bill from Thames Power for 12 Harbour View, London SE1 2AB, covering 1–31 March 2026. Account holder John Doe. Amount due £142.18, payable by 21 April 2026.",
      "keywords": ["proof of address", "address proof", "address verification", "..."],
      "created_at": "2026-09-22T18:33:47.636742641Z"
    }
  },
  {
    "type": "document",
    "score": 0.573,
    "matched_on": ["semantic"],
    "document": { "title": "Passport scan", "...": "..." }
  }
]
```

```bash
# 5. A mixed query — one list, ordered by score
curl -s 'localhost:8080/search?q=john&limit=3'
#   1.000 client    name,email   john.doe@neviswealth.com
#   0.900 document  content      Passport scan
#   0.900 document  content      Utility bill – March 2026

# 6. Errors are RFC 9457 problem+json
curl -s -X POST localhost:8080/clients -H 'Content-Type: application/json' -d '{"first_name":"","email":"nope"}'
# {"detail":"Request body failed validation","instance":"/clients","status":400,"title":"Bad Request",
#  "errors":{"email":"must be a well-formed email address","first_name":"must not be blank","last_name":"must not be blank"}}
```

---

## API

| Method | Path | Purpose | Success | Errors |
|---|---|---|---|---|
| `POST` | `/clients` | Create a client | `201` + `Location` | `400` validation, `409` email exists |
| `GET` | `/clients/{id}` | Fetch a client | `200` | `400` bad id, `404` |
| `POST` | `/clients/{id}/documents` | Add a document (embedded + enriched at write time) | `201` + `Location` | `400`, `404` client |
| `GET` | `/clients/{id}/documents` | List a client's documents | `200` | `404` client |
| `GET` | `/documents/{id}` | Fetch a document | `200` | `404` |
| `GET` | `/search?q=&type=&limit=` | Search clients and documents | `200` (list, possibly empty) | `400` blank `q`, bad `type`/`limit` |

* Swagger UI: `/docs` · OpenAPI JSON: `/v3/api-docs` (a copy is committed at [`docs/openapi.json`](docs/openapi.json))
* Markdown reference with every schema and the search semantics: [`docs/api.md`](docs/api.md)
* Health: `/actuator/health`

---

## How search works

Search runs as two independent matchers whose hits are merged into one list ordered by `score`.
Each hit says which fields matched (`matched_on`), so results are explainable.

**Clients: lexical.** Case-insensitive substring match over first name, last name, full name, email and
description, executed in SQL (`ILIKE` on a trigram-indexed expression). The score is *how much of the
matched field the query covers*: `john` against first name `John` is `1.0`; `NevisWealth` against
`john.doe@neviswealth.com` is scored against the domain part it actually hit (`11/15 ≈ 0.73`).

**Documents: three signals, computed once at write time and combined at query time.**

1. **Related search terms.** When a document is created it is *enriched* with keywords: synonyms, its
   document category and the purposes it serves. Two providers:
   * *Baseline (offline, default):* a curated domain thesaurus ([`thesaurus.json`](src/main/resources/thesaurus.json))
     of concept groups such as *proof of address · utility bill · electricity bill · council tax · bank statement*.
     A document mentioning any term of a group receives the others as keywords. Deterministic, testable, no network.
   * *Claude (when `ANTHROPIC_API_KEY` is set):* one call per document returns a summary and 5–15 search terms
     an advisor might type. Thesaurus keywords are merged in, so this is a strict superset of offline mode, and any
     failure (network, rate limit, refusal, malformed reply) silently falls back to it.
2. **Lexical match on tokens.** If every token of the query (lower-cased, stop words removed, plurals stripped)
   appears in the title, the keywords or the content, the document is relevant with certainty. Its score is lifted
   to at least `0.9` so literal hits rank first.
3. **Semantic similarity.** Title + content + keywords are embedded with `bge-small-en-v1.5` (384-d, runs
   in-process via ONNX, no external service) and stored in a pgvector `vector(384)` column with an HNSW index.
   At query time the query is embedded and the nearest neighbours are fetched with `cosine_distance` in HQL.
   Semantic-only hits are kept when similarity ≥ `MIN_DOCUMENT_SIMILARITY` (default `0.55`, chosen empirically:
   with this small model, unrelated queries top out around `0.5`).

Why not embeddings alone? Measured on the sample data, a small embedding model gives `address proof` →
*Utility bill* a similarity of `0.46`, indistinguishable from unrelated documents. The relation between a utility
bill and proof of address is domain knowledge, so the service captures it explicitly (thesaurus, or Claude), and
uses embeddings for the long tail of paraphrases (`identity document` → *Passport scan*, `investment performance`
→ *Portfolio review*).

**Response shape.** The spec says `/search` returns an array of objects; each object carries `type`
(`client` | `document`), `score` (0–1), `matched_on`, and the full `client` or `document`. `type=` and `limit=`
are optional filters.

---

## Running locally without Docker Compose

You need a PostgreSQL 16 with the `vector` extension available (e.g. `docker run -p 5432:5432 -e POSTGRES_PASSWORD=nevis -e POSTGRES_USER=nevis -e POSTGRES_DB=nevis pgvector/pgvector:pg16`) and JDK 21.

```bash
./gradlew bootRun                      # uses jdbc:postgresql://localhost:5432/nevis, user/password nevis
DB_URL=jdbc:postgresql://host:5432/db DB_USER=... DB_PASSWORD=... ./gradlew bootRun
```

Flyway creates the schema (and the `vector` / `pg_trgm` extensions) on first start.

## Tests

```bash
./gradlew test
```

* **Unit tests** cover the pure logic: tokenisation, thesaurus keyword extraction, client scoring and LIKE escaping,
  document scoring and thresholds, result ranking, extractive summaries, and parsing of Claude replies.
* **Integration tests** boot the whole application (real Flyway migrations, real pgvector queries, the real
  embedding model, offline enrichment) and drive it through MockMvc: every endpoint, every error code, both
  assignment examples, filters, limits, ordering, LIKE-wildcard safety, and the API-key filter.

By default the integration tests start `pgvector/pgvector:pg16` with Testcontainers (needs Docker). Without
Docker, point them at any PostgreSQL that has pgvector:

```bash
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/nevis_test TEST_DATABASE_USER=nevis TEST_DATABASE_PASSWORD=nevis ./gradlew test
```

The Claude enricher is not exercised against the live API in tests; its offline parts (prompt/response parsing,
fallback merging) are.

**CI** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) runs on every push and pull request: Gradle
wrapper validation, `./gradlew build` (all tests, against a `pgvector/pgvector:pg16` service container), and a
Docker Compose smoke test that builds the image, starts the stack and drives both assignment examples through the
real HTTP API.

---

## Design decisions and trade-offs

* **Enrich at write time, not query time.** Keywords, summary and embedding are computed when the document is
  created, so a search is one SQL query per matcher and never calls an LLM. Document creation is slower
  (~5 ms for the embedding; ~1–3 s with Claude) and, if enrichment logic changes, existing documents keep
  their old keywords until re-indexed. For an advisor tool (reads ≫ writes) that is the right side of the trade.
* **In-process embedding model.** A 384-d quantized ONNX model bundled in the jar means `docker compose up`
  works offline and reproducibly, with no API key or model download. The cost is a ~240 MB jar and modest
  quality; swapping to a hosted embedding API is a one-class change behind the `Embedder` interface.
* **pgvector instead of a vector database.** One store, one transaction, one Compose service. HNSW keeps
  nearest-neighbour search fast well beyond what this assignment needs.
* **Lexical + semantic, with explainable scores.** Pure vector similarity is hard to threshold and impossible
  to explain to an advisor. The `matched_on` field and the "literal hit ⇒ ≥ 0.9" rule make results predictable.
* **Explicit thesaurus + optional LLM.** The thesaurus makes the core promise ("similar terms") hold with no
  external dependency and makes it testable; Claude extends it beyond a hand-written list.
* **Claude Opus 5 at low effort, ~600 output tokens, 30 s timeout, one retry.** One call yields both the summary
  and the keywords. Any failure degrades to the baseline instead of failing the request.
* **Validation returns 400, not 422**, following Spring's convention; duplicates are 409; all errors are
  `application/problem+json`.
* **No pagination on `/search`**; `limit` (default 20, max 100) is enough for a type-ahead style UI.
* **Kept out of scope on purpose:** update/delete endpoints, authentication beyond a shared API key, async
  enrichment queue, per-advisor tenancy, re-indexing job. Each is a straightforward addition.

## Project layout

```
src/main/kotlin/com/nevis/search
├── api/            controllers, request/response DTOs, problem+json error handling
├── service/        ClientService, DocumentService (enrich + embed on write), SearchService (merge + rank)
├── search/         ClientMatcher, DocumentMatcher — pure scoring logic
├── enrichment/     DocumentEnricher: thesaurus keywords, extractive summary, Claude enricher, tokenizer
├── embedding/      Embedder interface + in-process bge-small implementation
├── repository/     Spring Data repositories; pgvector nearest-neighbour query (HQL cosine_distance)
├── domain/         JPA entities (Client, Document with vector(384) column)
└── config/         typed properties, OpenAPI, optional API-key filter
src/main/resources/db/migration   Flyway schema (extensions, tables, trigram + HNSW indexes)
src/main/resources/thesaurus.json Domain concept groups used for offline keyword enrichment
src/test/kotlin                   unit tests + full-stack integration tests
docs/                             openapi.json (generated), api.md
```
