================================================================================
                        DEVCOMPASS AI PLATFORM
      Internal Engineering System Discovery & RAG Knowledge Engine
================================================================================

DevCompass AI is an enterprise developer platform that enables software engineers
to rapidly discover and understand system architecture, team responsibilities,
application flows, database schemas, and CI/CD pipelines through AI-powered 
semantic search and RAG (Retrieval-Augmented Generation).

--------------------------------------------------------------------------------
1. ARCHITECTURE OVERVIEW
--------------------------------------------------------------------------------

+-------------------------+      +-------------------------------------------+      +-------------------------+
|   Knowledge Sources     |      |   Vector Store & Database                 |      |   LLM Answer Generator  |
|   Notion REST API       | ---> |   Amazon RDS PostgreSQL                   | ---> |   Google Gemini         |
|   Git Repositories      |      |   (pgvector + INFORMATION_SCHEMA)         |      |   (gemini-1.5-flash)    |
|   Database Metadata     |      |   Sliding Window Chunker (512/64 tokens)      |      |   Multi-Provider Strategy|
+-------------------------+      +-------------------------------------------+      +-------------------------+

--------------------------------------------------------------------------------
2. KEY FEATURES
--------------------------------------------------------------------------------

- Multi-Provider AI Architecture: Strategy pattern supporting Google Gemini, 
  ChatGPT (OpenAI), Claude, and local Ollama inference.
- Amazon RDS PostgreSQL pgvector: HNSW vector index storing 768-dimensional 
  embeddings generated locally via Ollama (nomic-embed-text).
- Sliding Window Chunking: Token-aware chunking (512 tokens / 64 overlap) to 
  prevent cross-boundary context loss during vector search.
- Live Notion REST API Integration: Traverses Notion page hierarchies starting 
  strictly from configured DevCompassAI root page.
- Incremental Background Sync: Periodic scheduled worker (@Async @Scheduled) 
  checking Notion last_edited_time vs PostgreSQL last_embedded_date.
- Live Database Schema Explorer: Dynamically queries PostgreSQL ANSI 
  INFORMATION_SCHEMA for tables, columns, primary keys, and foreign keys.

--------------------------------------------------------------------------------
3. PREREQUISITES & ENVIRONMENT VARIABLES
--------------------------------------------------------------------------------

Prerequisites:
- Java 21 LTS
- Maven 3.9+
- Ollama (running local nomic-embed-text model)
- Amazon RDS PostgreSQL (with pgvector extension enabled)

PowerShell Environment Variables:
$env:RDS_JDBC_URL="jdbc:postgresql://devcompassdb.cbkmsi648cor.ap-south-2.rds.amazonaws.com:5432/postgres?sslmode=require"
$env:RDS_USERNAME="postgres"
$env:RDS_PASSWORD="your-rds-password"
$env:GEMINI_API_KEY="AIzaSy..."
$env:NOTION_API_KEY="secret_..."
$env:NOTION_MAIN_PAGE_ID="3b2d87a1-7505-8034-b8e5-e76c150fc6bb"

--------------------------------------------------------------------------------
4. HOW TO RUN THE APPLICATION
--------------------------------------------------------------------------------

1. Start Ollama local embeddings:
   ollama serve
   ollama pull nomic-embed-text

2. Start Spring Boot Application:
   cd backend
   mvn spring-boot:run

   (Port: 8081 | Context Path: /)

--------------------------------------------------------------------------------
5. REST API ENDPOINTS
--------------------------------------------------------------------------------

- POST /api/v1/query
  Execute natural language RAG search queries.
  Payload: {"question": "SEO", "provider": "gemini", "topK": 5}

- GET /api/v1/pipeline/architecture
  Inspect 3-tier production RAG telemetry.

- GET /api/v1/pipeline/status
  Inspect total indexed vector chunk count and system status.

- POST /api/v1/pipeline/ingest
  Trigger automated full knowledge ingestion across Notion, Git, and Database.

- GET /api/v1/schema/tables
  Retrieve live Amazon RDS PostgreSQL table schemas from INFORMATION_SCHEMA.

- GET /api/v1/providers
  List registered LLM AI providers and active status.
