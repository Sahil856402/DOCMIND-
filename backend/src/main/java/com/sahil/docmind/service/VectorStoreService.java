package com.sahil.docmind.service;

import com.sahil.docmind.model.DocumentChunk;
import com.sahil.docmind.model.ScoredChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal in-memory vector store.
 * Good enough for a portfolio-scale RAG demo (hundreds of chunks).
 * For production scale, swap this out for Chroma / Pinecone / pgvector.
 */
@Service
public class VectorStoreService {

    private final List<DocumentChunk> store = new CopyOnWriteArrayList<>();

    public void add(DocumentChunk chunk) {
        store.add(chunk);
    }

    public void addAll(List<DocumentChunk> chunks) {
        store.addAll(chunks);
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }

    public List<DocumentChunk> getAll() {
        return store;
    }

    /**
     * Returns the top-K most similar chunks to the query embedding, ranked by cosine similarity.
     */
    public List<ScoredChunk> search(List<Double> queryEmbedding, int topK) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : store) {
            double score = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            scored.add(new ScoredChunk(chunk, score));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
