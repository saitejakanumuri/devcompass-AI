# DevCompass AI

DevCompass AI is a streamlined, user-account level Retrieval-Augmented Generation (RAG) platform. It allows users to connect their engineering knowledge sources (Git repositories, Notion workspaces, and Database schemas), automatically process them into vector embeddings, and query them using a powerful LLM via a clean, simple chat interface.
 
## 🚀 Features

- **User-Level RAG Architecture**: Each user has their own isolated knowledge configurations. When they ask questions, the system only searches across their ingested data.
- **Minimal Spring Security**: Secure JWT-based authentication (Login, Registration, Token Refresh) with clean BCrypt password hashing.
- **Supported Knowledge Sources**:
  - **Git Repositories**: Scans and chunks source code (`.java`, `.ts`, `.py`, `.md`, etc.).
  - **Notion Pages**: Ingests markdown from specified Notion pages or databases.
  - **Database Schemas**: Connects to an RDS PostgreSQL instance to extract tables and foreign key relationships for natural language DB queries.
- **Asynchronous Ingestion**: Background processes automatically fetch, chunk, and embed documents using local Ollama (Nomic text embeddings).
- **PGVector Storage**: Uses PostgreSQL `pgvector` for efficient similarity search.
- **Beginner-Friendly React Frontend**: A completely simplified React frontend (Vite + TypeScript) with just 3 clean pages (`Auth`, `Chat`, `Settings`) and no complex state management or bloated CSS.

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL (Amazon RDS) with `pgvector` extension
- **Security**: Spring Security + JWT
- **LLM Provider**: Configurable (Gemini, OpenAI, Claude, Ollama)
- **Embedding Provider**: Configurable (Ollama, OpenAI)

### Frontend
- **Framework**: React 19 (via Vite)
- **Language**: TypeScript
- **Styling**: Vanilla CSS (minimal and clean `index.css`)
- **HTTP Client**: Axios

## 📂 Project Structure

```
devcompass-ai/
├── backend/            # Spring Boot RAG Engine
│   ├── src/main/java/com/devcompass/ai/
│   │   ├── config/     # Spring Security & App configs
│   │   ├── controller/ # REST APIs (Auth, Query, Sources)
│   │   ├── model/      # Core Data Models (User, Chunk, Document, etc.)
│   │   ├── pipeline/   # RAG Pipeline (VectorStore, Chunker, EmbeddingGen)
│   │   ├── provider/   # LLM Answer Generators
│   │   ├── repository/ # Database Repositories (Users, Configs)
│   │   ├── security/   # JWT filters and auth logic
│   │   ├── service/    # Query Engine
│   │   └── source/     # Knowledge Extractors (Git, DB, Notion)
│   └── src/main/resources/
│       └── application.yml
└── frontend/           # React SPA
    ├── src/
    │   ├── api/        # Axios API Client
    │   ├── components/ # Reusable UI (Navbar)
    │   ├── pages/      # Views (AuthPage, ChatPage, SettingsPage)
    │   ├── types/      # TypeScript Interfaces
    │   ├── App.tsx     # Main Routing / Auth Gate
    │   └── index.css   # Global Styles
    └── package.json
```

## 🚀 Getting Started

### Prerequisites
- Java 21
- Node.js & npm
- PostgreSQL with `pgvector` installed
- Ollama (running locally with `nomic-embed-text` model for local embeddings)

### Backend Setup

1. **Configure Properties**: Open `backend/src/main/resources/application.yml` and configure your database and API keys (Gemini, Notion, etc.).
2. **Build and Run**:
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```
   The backend will start on `http://localhost:8081`.

### Frontend Setup

1. **Install Dependencies**:
   ```bash
   cd frontend
   npm install
   ```
2. **Run Development Server**:
   ```bash
   npm run dev
   ```
   The frontend will start on `http://localhost:5173`.

## 📖 Usage Flow

1. **Register/Login**: Create a new account and sign in.
2. **Configure Sources**: Go to the **Settings** tab and enter credentials/URLs for your Git, Notion, or Database sources. Click **Save & Sync**.
3. **Ask Questions**: Navigate to the **Chat** tab and ask questions like:
   - *"How is authentication handled in this repository?"*
   - *"What tables are related to user management in the database?"*
   - *"Summarize the API documentation from our Notion page."*

---

**Note:** This project was recently refactored to remove complex multi-tenant enterprise features (like Admin dashboards and company roles) in favor of a clean, beginner-friendly, and highly extensible user-level RAG framework.
