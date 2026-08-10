"Initially, I asked ChatGPT for AI project ideas, but generic suggestions like PR bots or basic chatbots didn't excite me—I wanted to solve a real engineering pain point.

The real spark came at work. A new teammate joined and naturally had dozens of questions about our system architecture, database relationships, CI/CD pipelines, and SEO frameworks. While I wanted to help, context-switching to answer these questions daily delayed my own task delivery, creating friction during morning standups.

Around the same time, while integrating a SaaS provider into our site, I noticed their documentation had an embedded AI search bar that provided instant, well-formatted answers from their API docs. That made me curious: 'How does it search and synthesize answers across huge documentation sets so fast?'

Diving into that question introduced me to the world of vector embeddings, semantic search, vector databases, and RAG architectures. I decided to build DevCompass AI to give engineering teams an intelligent, self-service platform for internal documentation and system discovery.

# DevCompass AI - Internal Engineering System Discovery Platform

**DevCompass AI** is an enterprise developer platform that enables software engineers to rapidly discover and understand system architecture, team responsibilities, application flows, database schemas, and CI/CD pipelines through AI-powered semantic search and Retrieval-Augmented Generation (RAG).

```
 ┌───────────────────────────┐      ┌─────────────────────────────────────────┐      ┌───────────────────────────┐
 │   Knowledge Sources       │      │   Vector Store & Database               │      │   LLM Answer Generator    │
 │   Live Notion REST API    │ ───► │   Amazon RDS PostgreSQL                 │ ───► │   Google Gemini (DEFAULT) │
 │   + Git Repos + SQL Schema│      │   INFORMATION_SCHEMA + pgvector         │      │   (gemini-1.5-flash)      │
 └───────────────────────────┘      └─────────────────────────────────────────┘      └───────────────────────────┘
```

---

## 🌟 Key Architecture Features

* **Multi-Provider Strategy Pattern**: Extensible AI abstraction supporting Google Gemini (`gemini-1.5-flash`), OpenAI (`gpt-4o`), Claude, and local Ollama.
* **Amazon RDS PostgreSQL `pgvector`**: Stores 768-dimensional vector embeddings with HNSW cosine distance indexing (`<=>`).
* **Local Embeddings via Ollama**: Uses `nomic-embed-text` running locally on port 11434.
* **Sliding Window Chunking**: Token-aware chunking (512 tokens / 64 overlap) ensuring boundary context preservation.
* **Notion REST API Root Traversal**: Confines crawling strictly to configured DevCompassAI main page hierarchy.
* **Incremental Background Sync Tracker**: Scheduled `@Async` worker checking Notion `last_edited_time` against PostgreSQL `last_embedded_date`.
* **Live Database Schema Explorer**: Queries ANSI `INFORMATION_SCHEMA` for tables, columns, primary keys, and foreign key relations.

---

## 🚀 Quick Start Guide

### 1. Start Ollama Embeddings Server
```bash
ollama serve
ollama pull nomic-embed-text
```

### 2. Configure Environment Variables (PowerShell)
```powershell
$env:RDS_JDBC_URL="jdbc:postgresql://localhost:5432/postgres?sslmode=require"
$env:RDS_USERNAME="postgres"
$env:RDS_PASSWORD="your-rds-password"
$env:GEMINI_API_KEY="AIzaSy..."
$env:NOTION_API_KEY="secret_..."
$env:NOTION_MAIN_PAGE_ID="3b2d87a1-7505-8034-b8e5-e76c150fc6bb"
```

### 3. Launch Spring Boot App
```powershell
cd backend
mvn spring-boot:run
```

---

## 📡 REST API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/query` | `POST` | Execute RAG search query with citations |
| `/api/v1/pipeline/architecture` | `GET` | Return 3-tier system architecture telemetry |
| `/api/v1/pipeline/status` | `GET` | Return total indexed vector chunk count |
| `/api/v1/pipeline/ingest` | `POST` | Trigger full knowledge ingestion sync |
| `/api/v1/schema/tables` | `GET` | Fetch live Amazon RDS table metadata |
| `/api/v1/providers` | `GET` | List registered AI providers |
