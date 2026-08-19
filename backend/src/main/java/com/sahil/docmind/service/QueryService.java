package com.sahil.docmind.service;

import com.sahil.docmind.model.ScoredChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QueryService {

    private final GeminiService geminiService;
    private final GroqService groqService;
    private final VectorStoreService vectorStoreService;

    @Value("${docmind.retrieval.topK}")
    private int topK;

    public QueryService(GeminiService geminiService, GroqService groqService, VectorStoreService vectorStoreService) {
        this.geminiService = geminiService;
        this.groqService = groqService;
        this.vectorStoreService = vectorStoreService;
    }

    public static class QueryResult {
        public String answer;
        public List<ScoredChunk> sources;

        public QueryResult(String answer, List<ScoredChunk> sources) {
            this.answer = answer;
            this.sources = sources;
        }
    }

    public QueryResult answer(String question) {
        if (vectorStoreService.size() == 0) {
            return new QueryResult(
                    "No documents have been uploaded yet. Please upload a PDF first.",
                    List.of()
            );
        }

        List<Double> questionEmbedding = geminiService.embedText(question);
        List<ScoredChunk> topChunks = vectorStoreService.search(questionEmbedding, topK);

        String context = topChunks.stream()
                .map(sc -> "[Source: " + sc.getChunk().getSourceFile()
                        + ", chunk " + sc.getChunk().getChunkIndex() + "]\n"
                        + sc.getChunk().getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
                You are a helpful assistant answering questions using ONLY the context below.
                If the answer is not contained in the context, say you don't have enough
                information rather than guessing.

                Context:
                %s

                Question: %s

                Write your answer in plain, well-organized prose — short paragraphs and,
                if needed, simple hyphen-based bullet points on their own lines. Do NOT use
                markdown tables, do NOT use ** for bold, and do NOT mention chunk numbers,
                filenames, or say things like "Source:" in your answer.
                """.formatted(context, question);

        String answer = groqService.generateAnswer(prompt);
        return new QueryResult(answer, topChunks);
    }
}