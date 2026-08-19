# DocMind — RAG Document Q&A Assistant

Upload a PDF, ask questions about it, get answers grounded in the actual document —
with source citations showing exactly which chunks were used and how similar they were
to your question.

Built as a companion project to [Resume IQ](https://github.com/Sahil856402/ResumeIQ-),
using the same stack: **Spring Boot + Gemini API + React**.

## How it works (RAG pipeline)

1. **Extract** — PDF text is pulled out with Apache PDFBox
2. **Chunk** — text is split into overlapping ~800-character chunks
3. **Embed** — each chunk is converted to a vector using Gemini's `text-embedding-004`
4. **Store** — vectors are held in an in-memory store (cosine similarity search)
5. **Retrieve** — on a question, it's embedded too, and the top-K most similar chunks are found
6. **Generate** — the question + retrieved chunks are sent to **Groq** (`llama-3.3-70b-versatile`),
   which answers using only that context

**Why two providers?** Gemini handles embeddings (free, no billing card needed for this part).
Groq handles generation (free tier, no billing card at all) — this sidesteps Gemini's
`generateContent` endpoint, which as of mid-2026 increasingly nudges free-tier users toward
linking a billing account. Mixing providers like this is also a legitimate, common real-world
pattern worth mentioning in interviews — it shows you can integrate multiple AI providers into
one pipeline.

## Project structure

```
DocMind/
├── backend/      Spring Boot API (Java 17, Maven)
│   └── src/main/java/com/sahil/docmind/
│       ├── controller/   DocumentController, QueryController
│       ├── service/      PdfExtraction, Chunking, Gemini, VectorStore, Ingestion, Query
│       ├── model/        DocumentChunk, ScoredChunk
│       └── config/       CORS + WebClient setup
└── frontend/     React + Vite UI
    └── src/App.jsx       Upload panel + Q&A panel with retrieval trace
```

## Setup

### 1. Backend

```bash
cd backend
```

Set **two** environment variables — one for Gemini (embeddings), one for Groq (generation):

**Gemini key** (embeddings only): from [Google AI Studio](https://aistudio.google.com/apikey)

**Groq key** (generation): from [console.groq.com/keys](https://console.groq.com/keys) —
sign up free, no card required, click "Create API Key"

```bash
# Windows PowerShell (run these in the SAME terminal you'll use for mvn spring-boot:run —
# env vars set in one terminal don't carry over to a new one)
$env:GEMINI_API_KEY="your-gemini-key-here"
$env:GROQ_API_KEY="your-groq-key-here"

# macOS/Linux
export GEMINI_API_KEY="your-gemini-key-here"
export GROQ_API_KEY="your-groq-key-here"
```

Then run:

```bash
mvn spring-boot:run
```

Backend starts on `http://localhost:8080`.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:5173`.

### 3. Try it

1. Open `http://localhost:5173`
2. Upload a PDF (a resume, a syllabus, any document you know well — so you can judge
   whether the answers are actually correct)
3. Ask a question about it
4. Check the retrieval trace to see which chunks were used and their similarity scores

## Notes on the in-memory vector store

This project uses a simple in-memory list with cosine similarity for the vector store —
good enough for a portfolio demo (hundreds of chunks). For production scale, swap
`VectorStoreService` for a real vector database like Chroma, Pinecone, or pgvector.
The rest of the pipeline (chunking, embedding, retrieval, prompt construction) stays
the same — that's the part worth understanding.

## API reference

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents/upload` | Upload a PDF (multipart, field name `file`) |
| GET | `/api/documents/status` | Check how many chunks are stored |
| DELETE | `/api/documents/clear` | Clear the vector store |
| POST | `/api/query` | `{ "question": "..." }` → answer + sources |

## Next steps to extend this

- Swap the in-memory store for Chroma or pgvector to show production-scale RAG
- Add evaluation: a small test set of Q&A pairs to measure hallucination rate
- Support multiple file types (docx, txt) via the existing chunking pipeline
- Add a "clear and re-upload" flow in the UI
- Deploy backend (Render/Railway) + frontend (Vercel/Netlify) with a live demo link —
  this matters more to recruiters than the repo alone
