# Nevis Search API — reference

Base URL: `http://localhost:8080`. All bodies are JSON with `snake_case` fields. Timestamps are RFC 3339 (UTC).
Interactive documentation: `/docs` (Swagger UI); machine-readable: `/v3/api-docs` or [`openapi.json`](openapi.json).

## Authentication

Off by default. When the server is started with `API_KEY=<secret>`, every request except `/docs`, `/v3/api-docs`
and `/actuator/health` must send `X-API-Key: <secret>`; otherwise the response is `401` with a problem body.

## Errors

Every error is `application/problem+json` (RFC 9457):

```json
{"status": 404, "title": "Not Found", "detail": "Client 9b1d… not found", "instance": "/clients/9b1d…"}
```

Validation errors add an `errors` object keyed by field:

```json
{"status": 400, "title": "Bad Request", "detail": "Request body failed validation", "instance": "/clients",
 "errors": {"email": "must be a well-formed email address", "first_name": "must not be blank"}}
```

| Status | When |
|---|---|
| `400` | Missing/blank/over-long fields, invalid email, malformed JSON, non-UUID id, blank `q`, unknown `type`, `limit` outside 1–100 |
| `401` | API key required and missing/wrong |
| `404` | Client or document id does not exist |
| `409` | A client with that email (case-insensitive) already exists |

---

## Clients

### `POST /clients` — create a client → `201 Created`

Request body:

| Field | Type | Required | Notes |
|---|---|---|---|
| `first_name` | string ≤ 255 | yes | |
| `last_name` | string ≤ 255 | yes | |
| `email` | string ≤ 255, email format | yes | unique, case-insensitive |
| `description` | string ≤ 10 000 | no | free text, searchable |
| `social_links` | string[] (≤ 20, each ≤ 2048) | no | not searchable |

Response: a **Client** (below). `Location: /clients/{id}`.

### `GET /clients/{id}` → `200` Client

### `GET /clients/{id}/documents` → `200` Document[] (creation order)

### Client

```json
{
  "id": "65a434b6-bad5-4294-aa72-1522fd16c959",
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@neviswealth.com",
  "description": "Retired surgeon, conservative risk profile.",
  "social_links": ["https://linkedin.com/in/johndoe"],
  "created_at": "2026-09-22T18:33:47.364880Z"
}
```
`description` is omitted when null.

---

## Documents

### `POST /clients/{id}/documents` — add a document → `201 Created`

| Field | Type | Required |
|---|---|---|
| `title` | string ≤ 255 | yes |
| `content` | string ≤ 200 000 | yes |

On creation the service:
1. **enriches** the document with a `summary` and related `keywords` (Claude when `ANTHROPIC_API_KEY` is set,
   otherwise the built-in thesaurus + the leading sentences of the content);
2. **embeds** title + content + keywords into a 384-d vector for semantic search.

Response: a **Document**. `Location: /documents/{id}`.

### `GET /documents/{id}` → `200` Document

### Document

```json
{
  "id": "40baec60-db21-43ac-9c8b-cc0836ce88bd",
  "client_id": "65a434b6-bad5-4294-aa72-1522fd16c959",
  "title": "Utility bill – March 2026",
  "content": "Electricity bill from Thames Power for 12 Harbour View …",
  "summary": "Electricity bill from Thames Power for 12 Harbour View …",
  "keywords": ["proof of address", "address proof", "address verification", "council tax", "bank statement"],
  "created_at": "2026-09-22T18:33:47.636742Z"
}
```
`summary` is omitted when none could be produced (e.g. enrichment disabled). `keywords` may be empty.

---

## Search

### `GET /search` → `200` SearchResult[]

| Query parameter | Required | Values |
|---|---|---|
| `q` | yes | free text, non-blank, ≤ 1000 chars |
| `type` | no | `client` or `document` (case-insensitive); both when omitted |
| `limit` | no | 1–100, default 20 |

The response is a single array, ordered by `score` descending (ties: clients first, then creation order),
truncated to `limit`. It is `[]` when nothing matches.

### SearchResult

Discriminated by `type`:

```json
{"type": "client",   "score": 0.733, "matched_on": ["email"],    "client":   { …Client… }}
{"type": "document", "score": 0.9,   "matched_on": ["keywords"], "document": { …Document… }}
```

| Field | Meaning |
|---|---|
| `type` | `client` or `document`; names the populated sub-object |
| `score` | relevance in `[0, 1]`, rounded to 3 decimals |
| `matched_on` | why it matched. Clients: any of `name`, `email`, `description`. Documents: `title`, `keywords`, `content` (literal token match) or `semantic` (embedding similarity only) |

### Matching rules

**Clients** match when the query is a case-insensitive substring of the first name, last name, full name, email
or description. `score` = query length ÷ length of the matched field (best sub-part of the email — local part or
domain — is used), clamped to `[0.05, 1]`. `%` and `_` in the query are literal.

**Documents** match when either
* every token of the query (lower-cased, stop words removed, plural `s` stripped) occurs in the title, the
  keywords or the content — `score = max(similarity, 0.9)`; or
* the cosine similarity between the query embedding and the document embedding is at least
  `MIN_DOCUMENT_SIMILARITY` (default `0.55`) — `score = similarity`, `matched_on = ["semantic"]`.

Examples on the sample data in the README:

| `q` | top result | `matched_on` |
|---|---|---|
| `NevisWealth` | client john.doe@neviswealth.com | `email` |
| `address proof` | Utility bill – March 2026 | `keywords` |
| `utility bill` | Utility bill – March 2026 | `title` |
| `identity document` | Passport scan | `keywords` |
| `KYC` | Passport scan | `content` |
| `investment performance` | Q1 2026 portfolio review | `keywords` |
| `pizza recipe` | *(no results)* | |
