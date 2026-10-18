-- RAG knowledge base for the AI guest assistant (phase 7): text chunks + their embeddings.
-- pgvector ("vector" extension) is part of the pgvector/pgvector image; on managed Postgres it must be
-- allow-listed (Azure: azure.extensions = VECTOR,BTREE_GIST).
CREATE EXTENSION IF NOT EXISTS vector;

-- Layout expected by Spring AI's PgVectorStore (schema creation is disabled there: Flyway owns it).
CREATE TABLE vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(384) -- paraphrase-multilingual-MiniLM-L12-v2 produces 384 numbers per text
);

-- HNSW: approximate nearest-neighbour index for cosine distance (fast even with many chunks).
CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding vector_cosine_ops);
