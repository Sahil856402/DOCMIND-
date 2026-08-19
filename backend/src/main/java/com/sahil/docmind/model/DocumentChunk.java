package com.sahil.docmind.model;

import java.util.List;

public class DocumentChunk {

    private String id;
    private String sourceFile;
    private int chunkIndex;
    private String text;
    private List<Double> embedding;

    public DocumentChunk() {}

    public DocumentChunk(String id, String sourceFile, int chunkIndex, String text, List<Double> embedding) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.embedding = embedding;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
}
