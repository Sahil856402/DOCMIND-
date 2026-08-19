package com.sahil.docmind.model;

public class ScoredChunk {
    private DocumentChunk chunk;
    private double score;

    public ScoredChunk(DocumentChunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }

    public DocumentChunk getChunk() { return chunk; }
    public double getScore() { return score; }
}
