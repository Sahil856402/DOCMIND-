package com.sahil.docmind.service;

import com.sahil.docmind.model.DocumentChunk;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentIngestionService {

    private final PdfExtractionService pdfExtractionService;
    private final ChunkingService chunkingService;
    private final GeminiService geminiService;
    private final VectorStoreService vectorStoreService;

    public DocumentIngestionService(PdfExtractionService pdfExtractionService,
                                     ChunkingService chunkingService,
                                     GeminiService geminiService,
                                     VectorStoreService vectorStoreService) {
        this.pdfExtractionService = pdfExtractionService;
        this.chunkingService = chunkingService;
        this.geminiService = geminiService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Full ingestion pipeline: extract -> chunk -> embed -> store.
     * Returns the number of chunks created.
     */
    public int ingest(MultipartFile file) throws IOException {
        String rawText = pdfExtractionService.extractText(file);
        List<String> textChunks = chunkingService.chunk(rawText);

        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);
            List<Double> embedding = geminiService.embedText(chunkText);
            DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID().toString(),
                    file.getOriginalFilename(),
                    i,
                    chunkText,
                    embedding
            );
            documentChunks.add(chunk);
        }

        vectorStoreService.addAll(documentChunks);
        return documentChunks.size();
    }
}
