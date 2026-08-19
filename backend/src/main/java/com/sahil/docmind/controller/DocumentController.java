package com.sahil.docmind.controller;

import com.sahil.docmind.service.DocumentIngestionService;
import com.sahil.docmind.service.VectorStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final VectorStoreService vectorStoreService;

    public DocumentController(DocumentIngestionService ingestionService,
                               VectorStoreService vectorStoreService) {
        this.ingestionService = ingestionService;
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are supported"));
        }

        try {
            // Clear previous document(s) so answers always come from the most recently
            // uploaded PDF only, instead of mixing chunks from every upload in this session.
            vectorStoreService.clear();

            int chunkCount = ingestionService.ingest(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Document ingested successfully",
                    "filename", file.getOriginalFilename(),
                    "chunksCreated", chunkCount,
                    "totalChunksInStore", vectorStoreService.size()
            ));
        } catch (Exception e) {
            String rawMessage = e.getMessage();
            String message;
            if (rawMessage != null && rawMessage.contains("trailer")) {
                message = "This PDF couldn't be read (it may be corrupted, scanned oddly, or not a standard PDF). Try re-saving/exporting it as a fresh PDF and uploading again.";
            } else {
                message = rawMessage;
            }
            return ResponseEntity.internalServerError().body(Map.of("error", message));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("totalChunksInStore", vectorStoreService.size()));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear() {
        vectorStoreService.clear();
        return ResponseEntity.ok(Map.of("message", "Vector store cleared"));
    }
}