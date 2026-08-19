package com.sahil.docmind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    @Value("${docmind.chunk.size}")
    private int chunkSize;

    @Value("${docmind.chunk.overlap}")
    private int overlap;

    /**
     * Splits text into overlapping chunks by character count.
     * Overlap ensures we don't lose context at chunk boundaries.
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String cleaned = text.replaceAll("\\s+", " ").trim();

        if (cleaned.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + chunkSize, cleaned.length());
            chunks.add(cleaned.substring(start, end));

            if (end == cleaned.length()) {
                break;
            }
            start = end - overlap;
        }

        return chunks;
    }
}
