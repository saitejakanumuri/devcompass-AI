================================================================================
                       DEVCOMPASS AI PLATFORM
         Internal Engineering Knowledge Retrieval & RAG Engine
================================================================================

DevCompass AI is an enterprise developer platform that enables software engineers
to rapidly discover and understand system architecture, team responsibilities,
application flows, database schemas, and CI/CD pipelines through AI-powered
semantic search and RAG (Retrieval-Augmented Generation).

================================================================================
1. ARCHITECTURE OVERVIEW
================================================================================

+-------------------------+    +------------------------------------------+    +-------------------------+
|   Knowledge Sources     |    |   Vector Store & Database                |    |   LLM Answer Generator  |
|   Notion REST API       | -> |   Amazon RDS PostgreSQL (pgvector)       | -> |   Google Gemini         |
|   Git Repositories      |    |   HNSW cosine index (768-dim, <=> op)    |    |   OpenAI GPT-4o         |
|   ANSI SQL Schema       |    |   + INFORMATION_SCHEMA explorer          |    |   Ollama (local)        |
+-------------------------+    |   + knowledge_sync_tracker               |    |   Plug-and-play Strategy|
                               |   + notion_webhook_queue                 |    +-------------------------+
                               +------------------------------------------+

================================================================================
2. KEY FEATURES
================================================================================

- Plug-and-Play Strategy Pattern: Hot-swappable LLM providers (Gemini, OpenAI,
  Ollama) and embedding engines with zero code changes to switch.

- HNSW-Indexed Cosine Similarity: 768-dim embeddings stored in pgvector with
  score-threshold filtering to return only contextually relevant chunks.

- Local Embeddings via Ollama: nomic-embed-text on port 11434 — no external
  embedding API calls or costs.

- Sliding Window Chunking: Token-aware chunking (512 tokens / 64 overlap)
  preserving boundary context across splits.

- Async Notion Webhook Queue: Notion page-update events are enqueued in
  notion_webhook_queue (PostgreSQL) and processed by @Scheduled + @Async
  worker polling every 15 seconds.

- Account-Level Bounded Context: Each user's source credentials (userId,
  sourceType, configJson, status, lastSyncedAt) are stored in
  account_knowledge_configs. Vector embeddings in vector_chunks are scoped
  by user_id (FK to users table).

- Incremental Sync Tracker: knowledge_sync_tracker records last_edited_time
  per document — re-embedding only triggered on actual content changes.

- Live Database Schema Explorer: Queries ANSI INFORMATION_SCHEMA for tables,
  columns, primary keys, and foreign key relationships on any JDBC source.

- JWT-Based Auth: User registration and login with SHA-256 password hashing
  and Base64-encoded session token generation.

- Admin Dashboard: Platform-wide telemetry — total users, active configs,
  indexed chunks, and source sync status.

- Onboarding Flows API: Returns structured onboarding steps to guide new
  engineers through platform setup and usage.

================================================================================
3. DATABASE SCHEMA (AUTO-INITIALIZED ON STARTUP)
================================================================================

Table                      | Purpose
---------------------------|-------------------------------------------------------
users                      | User accounts with hashed passwords and roles
vector_chunks              | 768-dim pgvector embeddings scoped by user_id
account_knowledge_configs  | Per-user source credentials (sourceType, configJson)
knowledge_sync_tracker     | Document-level sync state (last_edited_time, chunks)
notion_webhook_queue       | Async queue: PENDING -> PROCESSING -> COMPLETED/FAILED

================================================================================
4. PREREQUISITES & ENVIRONMENT VARIABLES
================================================================================

Prerequisites:
  - Java 21 LTS
  - Maven 3.9+
  - Node.js 18+ (for frontend)
  - Ollama (running nomic-embed-text locally)
  - Amazon RDS PostgreSQL with pgvector extension enabled

PowerShell Environment Variables:
  $env:RDS_JDBC_URL="jdbc:postgresql://devcompassdb.cbkmsi648cor.ap-south-2.rds.amazonaws.com:5432/postgres?sslmode=require"
  $env:RDS_USERNAME="postgres"
  $env:RDS_PASSWORD="your-rds-password"
  $env:GEMINI_API_KEY="AIzaSy..."
  $env:NOTION_API_KEY="secret_..."
  $env:NOTION_MAIN_PAGE_ID="your-notion-page-id"

================================================================================
5. HOW TO RUN
================================================================================

1. Start Ollama local embeddings:
     ollama serve
     ollama pull nomic-embed-text

2. Start Spring Boot backend:
     cd backend
     mvn spring-boot:run
     (Port: 8081)

3. Start React frontend:
     cd frontend
     npm install
     npm run dev

================================================================================
6. REST API ENDPOINTS
================================================================================

AUTH
  POST /api/v1/auth/register
    Register a new user. Body: { "email", "password", "fullName" }

  POST /api/v1/auth/login
    Authenticate user. Body: { "email", "password" }

QUERY (RAG)
  POST /api/v1/query
    Execute a natural-language RAG search.
    Body: { "question": "...", "provider": "gemini", "topK": 5 }

PIPELINE
  POST /api/v1/pipeline/ingest
    Trigger full multi-source knowledge ingestion.

  GET  /api/v1/pipeline/status
    Return total indexed chunk count and active provider info.

  GET  /api/v1/pipeline/architecture
    Return 3-tier RAG architecture telemetry (embedding, vector DB, LLM).

KNOWLEDGE SOURCES
  GET  /api/v1/sources
    List all registered knowledge sources and health status.

  POST /api/v1/sources/sync/{sourceType}
    Sync a specific source. sourceType: GIT_REPOSITORY | NOTION | DATABASE_METADATA

ACCOUNT CONFIG (per-user scoped sources)
  GET  /api/v1/account/configs
    Fetch all knowledge source configs for the authenticated user.
    Headers: X-User-Id or X-Account-Id

  POST /api/v1/account/config
    Save or update a source config for the authenticated user.

  POST /api/v1/account/config/{sourceType}/sync
    Trigger ingestion scoped to the user's own credentials.

  POST /api/v1/account/config/{sourceType}/test
    Test connectivity for the user's configured source.

GIT REPOSITORY
  GET  /api/v1/git/config
    Get current Git repository configuration.

  POST /api/v1/git/config
    Update config (repoUrl, repoPath, branch, includedExtensions, maxFileSizeKb).
    Query param: autoSync=true to immediately trigger ingestion.

  POST /api/v1/git/sync
    Trigger Git repository ingestion manually.

SCHEMA EXPLORER
  GET  /api/v1/schema/tables
    Fetch live table metadata from ANSI INFORMATION_SCHEMA.

NOTION WEBHOOKS
  POST /api/v1/webhooks/notion
    Receive Notion page-update events. Handles verification handshake
    (verification_code, challenge, verification_token) and enqueues
    page sync tasks into notion_webhook_queue for async processing.

PROVIDERS & ONBOARDING
  GET  /api/v1/providers
    List all registered LLM AI providers and active status.

  GET  /api/v1/onboarding/flows
    Return structured onboarding steps for new engineers.

  GET  /api/v1/admin/stats
    Return platform-wide admin telemetry.

================================================================================
7. TECH STACK
================================================================================

  Backend    : Spring Boot 3, Java 21, Spring MVC, Hibernate ORM
  Vector DB  : Amazon RDS PostgreSQL + pgvector extension
  Embeddings : Ollama (nomic-embed-text, 768-dim)
  LLM        : Google Gemini, OpenAI GPT-4o, Ollama (Strategy Pattern)
  Frontend   : React 18, TypeScript, Vite
  Auth       : SHA-256 + Base64 token
  Build      : Maven 3.9+

================================================================================
