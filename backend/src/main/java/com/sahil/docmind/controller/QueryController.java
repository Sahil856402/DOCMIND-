package com.sahil.docmind.controller;

import com.sahil.docmind.model.ScoredChunk;
import com.sahil.docmind.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    public record QueryRequest(String question) {}

    @PostMapping
    public ResponseEntity<?> ask(@RequestBody QueryRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question must not be empty"));
        }

        try {
            QueryService.QueryResult result = queryService.answer(request.question());

            List<Map<String, Object>> sources = result.sources.stream()
                    .map(sc -> Map.<String, Object>of(
                            "sourceFile", sc.getChunk().getSourceFile(),
                            "chunkIndex", sc.getChunk().getChunkIndex(),
                            "similarity", Math.round(sc.getScore() * 1000.0) / 1000.0,
                            "excerpt", sc.getChunk().getText().length() > 200
                                    ? sc.getChunk().getText().substring(0, 200) + "..."
                                    : sc.getChunk().getText()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "answer", result.answer,
                    "sources", sources
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
