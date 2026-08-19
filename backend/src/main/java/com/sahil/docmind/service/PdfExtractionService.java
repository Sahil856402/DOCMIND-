package com.sahil.docmind.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfExtractionService {

    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().length() < 20) {
                throw new IOException(
                    "No readable text found in this PDF. It may be a scanned image " +
                    "without OCR, or contain only images/graphics. Try a text-based PDF instead."
                );
            }

            return text;
        }
    }
}