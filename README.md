"Initially, I asked ChatGPT for AI project ideas, but generic suggestions like PR bots or basic chatbots didn't excite me—I wanted to solve a real engineering pain point.

The real spark came at work. A new teammate joined and naturally had dozens of questions about our system architecture, database relationships, CI/CD pipelines, and SEO frameworks. While I wanted to help, context-switching to answer these questions daily delayed my own task delivery, creating friction during morning standups.

Around the same time, while integrating a SaaS provider into our site, I noticed their documentation had an embedded AI search bar that provided instant, well-formatted answers from their API docs. That made me curious: 'How does it search and synthesize answers across huge documentation sets so fast?'

Diving into that question introduced me to the world of vector embeddings, semantic search, vector databases, and RAG architectures. I decided to build DevCompass AI to give engineering teams an intelligent, self-service platform for internal documentation and system discovery."

---

# DevCompass AI — Internal Engineering Knowledge Retrieval Platform

**DevCompass AI** is an enterprise developer platform that enables software engineers to rapidly discover and understand system architecture, team responsibilities, application flows, database schemas, and CI/CD pipelines through AI-powered semantic search and Retrieval-Augmented Generation (RAG).

```
 ┌─────────────────────────┐      ┌────────────────────────────────────────┐      ┌──────────────────────────┐
 │   Knowledge Sources     │      │   Vector Store & Database              │      │   LLM Answer Generator   │
 │   Notion REST API       │ ───► │   Amazon RDS PostgreSQL (pgvector)     │ ───► │   Google Gemini (default)│
 │   Git Repositories      │      │   HNSW cosine index (768-dim)          │      │   OpenAI / Ollama        │
 │   ANSI SQL Schema       │      │   + INFORMATION_SCHEMA explorer        │      │   Plug-and-play Strategy │
 └─────────────────────────┘      └────────────────────────────────────────┘      └──────────────────────────┘
```

---

## 🌟 Key Architecture Features

- **Plug-and-Play Strategy Pattern**: Hot-swappable AI providers (Google Gemini, OpenAI GPT-4o, local Ollama) and embedding engines — no code changes needed to switch.
- **HNSW-Indexed Cosine Similarity Search**: Stores 768-dimensional vector embeddings in Amazon RDS PostgreSQL (`pgvector`) with `<=>` operator and score-threshold filtering to return only contextually relevant chunks.
- **Local Embeddings via Ollama**: Generates embeddings with `nomic-embed-text` running locally on port `11434` — no external embedding API calls or costs.
- **Sliding Window Chunking**: Token-aware chunking (512 tokens / 64 overlap) preserving boundary context across document splits.
- **Async Notion Webhook Queue**: Notion page-update events are enqueued into `notion_webhook_queue` (PostgreSQL) and processed by a `@Scheduled + @Async` worker polling every 15 seconds.
- **Account-Level Bounded Context**: Each user's knowledge source credentials (`userId`, `sourceType`, `configJson`, `status`, `lastSyncedAt`) are stored in `AccountKnowledgeConfig` — vector embeddings in `vector_chunks` are scoped by `user_id` (foreign key to `users`).
- **Incremental Sync Tracker**: `knowledge_sync_tracker` table records `last_edited_time` per document — re-embedding only triggered when content actually changes.
- **Live Database Schema Explorer**: Queries ANSI `INFORMATION_SCHEMA` for tables, columns, primary keys, and foreign key relationships against any configured JDBC data source.
- **JWT-Based Auth**: User registration and login with SHA-256 password hashing and Base64-encoded token generation.
- **Admin Dashboard**: Aggregated platform telemetry — total users, active configs, indexed chunks, and source sync status.
- **Onboarding Flows API**: Returns structured onboarding steps to guide new engineers through the platform.

---

## 🚀 Quick Start Guide

### 1. Start Ollama Embeddings Server
```bash
ollama serve
ollama pull nomic-embed-text
```

### 2. Configure Environment Variables (PowerShell)
```powershell
$env:RDS_JDBC_URL="jdbc:postgresql://devcompassdb.cbkmsi648cor.ap-south-2.rds.amazonaws.com:5432/postgres?sslmode=require"
$env:RDS_USERNAME="postgres"
$env:RDS_PASSWORD="your-rds-password"
$env:GEMINI_API_KEY="AIzaSy..."
$env:NOTION_API_KEY="secret_..."
$env:NOTION_MAIN_PAGE_ID="your-notion-page-id"
```

### 3. Launch Spring Boot App
```powershell
cd backend
mvn spring-boot:run
```
> Default port: **8081**

### 4. Launch React Frontend
```powershell
cd frontend
npm install
npm run dev
```

---

## 📡 REST API Reference

### Auth
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/auth/register` | `POST` | Register a new user account |
| `/api/v1/auth/login` | `POST` | Authenticate and receive a session token |

### Query (RAG)
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/query` | `POST` | Execute a natural-language RAG search query with citations |

### Pipeline
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/pipeline/ingest` | `POST` | Trigger full multi-source knowledge ingestion |
| `/api/v1/pipeline/status` | `GET` | Return total indexed chunk count and system status |
| `/api/v1/pipeline/architecture` | `GET` | Return 3-tier RAG architecture telemetry |

### Knowledge Sources
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/sources` | `GET` | List all registered knowledge sources and health status |
| `/api/v1/sources/sync/{sourceType}` | `POST` | Trigger sync for a specific source (`GIT_REPOSITORY`, `NOTION`, `DATABASE_METADATA`) |

### Account Config (per-user knowledge sources)
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/account/configs` | `GET` | Fetch all knowledge source configs for the authenticated user |
| `/api/v1/account/config` | `POST` | Save or update a source config for the authenticated user |
| `/api/v1/account/config/{sourceType}/sync` | `POST` | Trigger per-user source ingestion scoped to user's credentials |
| `/api/v1/account/config/{sourceType}/test` | `POST` | Test connectivity for a configured source |

### Git Repository
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/git/config` | `GET` | Get current Git repository configuration |
| `/api/v1/git/config` | `POST` | Update Git repo config (URL, path, branch, extensions) |
| `/api/v1/git/sync` | `POST` | Trigger Git repository ingestion |

### Schema Explorer
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/schema/tables` | `GET` | Fetch live table metadata from ANSI `INFORMATION_SCHEMA` |

### Notion Webhooks
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/webhooks/notion` | `POST` | Receive Notion page-update events and enqueue for async re-embedding |

### Providers & Onboarding
| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/providers` | `GET` | List registered LLM AI providers and active status |
| `/api/v1/onboarding/flows` | `GET` | Return structured onboarding steps for new engineers |
| `/api/v1/admin/stats` | `GET` | Return platform-wide admin telemetry |

---

## 🗄️ Database Schema (Auto-initialized on Startup)

| Table | Purpose |
|---|---|
| `users` | User accounts with hashed passwords and roles |
| `vector_chunks` | 768-dim pgvector embeddings scoped by `user_id` and `source_type` |
| `account_knowledge_configs` | Per-user source credentials (`sourceType`, `configJson`, `status`, `lastSyncedAt`) |
| `knowledge_sync_tracker` | Document-level sync state (`last_edited_time`, `last_embedded_date`, `chunk_count`) |
| `notion_webhook_queue` | Async queue for Notion webhook events (`PENDING → PROCESSING → COMPLETED/FAILED`) |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3, Java 21, Spring MVC, Hibernate ORM |
| Vector DB | Amazon RDS PostgreSQL + pgvector extension |
| Embeddings | Ollama (`nomic-embed-text`, 768-dim) |
| LLM Providers | Google Gemini, OpenAI GPT-4o, Ollama (Strategy Pattern) |
| Frontend | React 18, TypeScript, Vite |
| Auth | SHA-256 + Base64 token |
| Build | Maven 3.9+ |
